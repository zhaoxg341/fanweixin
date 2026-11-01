package cn.itcast.weixin.utils;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 局域网聊天客户端。
 *
 * 干的事很朴素：开一个后台线程连上电脑上的 ChatServer，
 * 用 BufferedReader 一行一行地读消息（协议是"一行一条 JSON"，
 * 靠换行分帧，所以完全不用处理粘包/拆包），
 * 读到之后把结果扔回主线程交给 ChatManager 处理。
 *
 * 断线会自动重连（每 3 秒试一次），连上后自动补发一条 login 告诉服务器"我是谁"。
 */
public class ChatClient {

    /** 连接状态与服务器通知的回调，全部在主线程触发 */
    public interface Listener {
        void onConnectionChanged(boolean connected, String hint);

        void onNotice(String message);
    }

    private static final long CONNECT_TIMEOUT = 5000L;
    private static final long RECONNECT_DELAY = 3000L;

    private static final ChatClient INSTANCE = new ChatClient();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private volatile Socket socket;
    private volatile BufferedWriter writer;
    private volatile boolean running = false;
    private volatile boolean connected = false;

    /**
     * 连接代次。每次 connect / disconnect 都会 +1，
     * 工作线程只在自己那一代还有效时才继续重连 —— 这样"改完服务器地址重新连接"时，
     * 旧线程不会醒过来又连一次，避免出现两条连接。
     */
    private volatile int generation = 0;

    private String host;
    private int port;
    private String myNick;
    private Thread worker;

    private ChatClient() {
    }

    public static ChatClient get() {
        return INSTANCE;
    }

    public boolean isConnected() {
        return connected;
    }

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /** 按当前设置自动连接（只在局域网模式且填了服务器地址时生效） */
    public void connectIfNeeded() {
        if (!AppPrefs.isLanMode()) {
            return;
        }
        String serverHost = AppPrefs.getServerHost();
        if (serverHost == null || serverHost.trim().isEmpty()) {
            return;
        }
        connect(serverHost.trim(), AppPrefs.getServerPort(), AppPrefs.getMyNick());
    }

    /** 开始连接。已在运行中就直接返回，重复调用是安全的 */
    public synchronized void connect(String serverHost, int serverPort, String nick) {
        if (running) {
            return;
        }
        this.host = serverHost;
        this.port = serverPort;
        this.myNick = nick;
        this.running = true;
        final int myGeneration = ++generation;
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                loop(myGeneration);
            }
        }, "chat-client");
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void disconnect() {
        running = false;
        generation++;
        closeQuietly();
        setConnected(false, "已断开连接");
    }

    /** 发一条消息给对方（对方昵称由服务器查找） */
    public void send(String to, String text) {
        if (!connected) {
            notifyNotice("还没连上服务器，消息没有发出去");
            return;
        }
        if (to == null || to.trim().isEmpty()) {
            return;
        }
        sendLine(buildJson("msg", to, text));
    }

    // ---------------- 以下都是后台线程里跑的 ----------------

    private void loop(int myGeneration) {
        while (running && myGeneration == generation) {
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(host, port), (int) CONNECT_TIMEOUT);
                s.setTcpNoDelay(true);
                socket = s;
                writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"));
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(s.getInputStream(), "UTF-8"));

                // 连上第一件事：报上自己的昵称，服务器据此建立"昵称 → 连接"的映射
                sendLine(buildJson("login", null, ""));

                setConnected(true, "已连接服务器 " + host + ":" + port);

                String line;
                while (running && myGeneration == generation
                        && (line = reader.readLine()) != null) {
                    handleLine(line);
                }
            } catch (Exception e) {
                // 连不上或中途断开，走下面的重连逻辑
            } finally {
                closeQuietly();
                if (myGeneration == generation) {
                    setConnected(false, "连接已断开，正在重试…");
                }
            }
            if (!running || myGeneration != generation) {
                break;
            }
            try {
                Thread.sleep(RECONNECT_DELAY);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /** 一行一条 JSON：{"type":"msg","from":"张三","text":"你好"} */
    private void handleLine(String line) {
        try {
            JSONObject obj = new JSONObject(line);
            String type = obj.optString("type");
            if ("msg".equals(type)) {
                final String from = obj.optString("from");
                final String text = obj.optString("text");
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        ChatManager.receiveRemote(from, text);
                    }
                });
            } else if ("notice".equals(type)) {
                final String text = obj.optString("text");
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyNotice(text);
                    }
                });
            }
        } catch (Exception e) {
            // 收到不认识的格式，忽略这一行
        }
    }

    private synchronized void sendLine(String line) {
        BufferedWriter w = writer;
        if (w == null) {
            return;
        }
        try {
            w.write(line);
            w.write("\n");
            w.flush();
        } catch (Exception e) {
            // 写失败说明连接已断，交给 loop 里的重连处理
        }
    }

    /** 拼一条协议 JSON。换行会破坏"一行一条"的分帧，所以要清掉 */
    private String buildJson(String type, String to, String text) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", type);
            obj.put("from", myNick == null ? "" : myNick);
            if (to != null) {
                obj.put("to", to);
            }
            obj.put("text", text == null ? "" : text.replace("\n", " ").replace("\r", " "));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj.toString();
    }

    private void closeQuietly() {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (Exception e) {
            // 忽略
        }
        writer = null;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            // 忽略
        }
        socket = null;
    }

    /** 只在状态真正变化时才回调，避免重连循环里反复弹提示 */
    private void setConnected(final boolean value, final String hint) {
        if (connected == value) {
            return;
        }
        connected = value;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (Listener listener : listeners) {
                    listener.onConnectionChanged(value, hint);
                }
            }
        });
    }

    private void notifyNotice(final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (Listener listener : listeners) {
                    listener.onNotice(message);
                }
            }
        });
    }
}
