import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 局域网聊天服务端（配套 WeiXinDemo 的"两个人线上聊天"）。
 *
 * 这是个普通 Java SE 程序，不需要 Tomcat、不需要任何第三方库，
 * 直接 javac + java 就能跑，跑在电脑上；两台手机连同一个 WiFi，填上电脑的局域网 IP 就能互发消息。
 *
 * 协议：一行一条 JSON（用换行分帧，所以不存在粘包/拆包问题）
 *   客户端 → 服务端   {"type":"login","from":"张三","text":""}
 *   客户端 → 服务端   {"type":"msg","from":"张三","to":"李思思","text":"你好"}
 *   服务端 → 客户端   {"type":"msg","from":"张三","text":"你好"}
 *   服务端 → 客户端   {"type":"notice","text":"「李思思」不在线"}
 *
 * 服务端只做一件事：维护"昵称 → 连接"的表，收到消息就查表转发。不做账号、不做离线存储。
 *
 * 用法：
 *   javac -encoding UTF-8 ChatServer.java
 *   java ChatServer            # 默认端口 8888
 *   java ChatServer 9000       # 指定端口
 */
public class ChatServer {

    /** 在线用户表：昵称 → 该用户的连接 */
    private static final Map<String, ClientHandler> ONLINE = new ConcurrentHashMap<>();

    private static final SimpleDateFormat LOG_FMT = new SimpleDateFormat("HH:mm:ss");

    public static void main(String[] args) throws Exception {
        int port = 8888;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("端口参数不是数字，改用默认端口 8888");
            }
        }

        ServerSocket server = new ServerSocket(port);
        printStartupInfo(port);

        ExecutorService pool = Executors.newCachedThreadPool();
        while (true) {
            Socket socket = server.accept();
            pool.execute(new ClientHandler(socket));
        }
    }

    /** 打印本机所有局域网 IP，方便在手机上填写 */
    private static void printStartupInfo(int port) throws Exception {
        System.out.println("========================================");
        System.out.println(" WeiXinDemo 聊天服务端已启动");
        System.out.println(" 端口: " + port);
        System.out.println("----------------------------------------");
        System.out.println(" 在手机的「登录」页里，服务器地址填下面任意一个 IP：");
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        boolean found = false;
        for (NetworkInterface ni : Collections.list(interfaces)) {
            if (ni.isLoopback() || !ni.isUp()) {
                continue;
            }
            for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                InetAddress addr = ia.getAddress();
                if (addr instanceof Inet4Address) {
                    System.out.println("   " + addr.getHostAddress() + "    (" + ni.getDisplayName() + ")");
                    found = true;
                }
            }
        }
        if (!found) {
            System.out.println("   （没找到局域网 IP，确认电脑已连上 WiFi）");
        }
        System.out.println("----------------------------------------");
        System.out.println(" 手机和电脑必须连同一个 WiFi。");
        System.out.println(" 如果连不上，检查 Windows 防火墙是否放行了 Java。");
        System.out.println("========================================");
        System.out.println();
    }

    private static void log(String message) {
        System.out.println("[" + LOG_FMT.format(new Date()) + "] " + message);
    }

    /** 每个客户端一个线程 */
    private static class ClientHandler implements Runnable {

        private final Socket socket;
        private BufferedReader reader;
        private BufferedWriter writer;

        /** 登录后的昵称，未登录时为 null */
        private String user;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    handle(line);
                }
            } catch (Exception e) {
                // 客户端异常断开，正常现象，不用打印堆栈
            } finally {
                offline();
            }
        }

        private void handle(String line) {
            String type = jsonGet(line, "type");
            if ("login".equals(type)) {
                String nick = jsonGet(line, "from");
                if (nick == null || nick.trim().isEmpty()) {
                    sendNotice("昵称不能为空");
                    return;
                }
                user = nick.trim();
                ClientHandler old = ONLINE.put(user, this);
                if (old != null && old != this) {
                    old.sendNotice("你的账号在别处登录了");
                    old.closeQuietly();
                }
                log("上线: " + user + "  (" + socket.getRemoteSocketAddress() + ")  当前在线 " + ONLINE.size() + " 人");
                sendNotice("已连接服务器。当前在线：" + onlineNames());
                return;
            }

            if ("msg".equals(type)) {
                String text = jsonGet(line, "text");
                String to = jsonGet(line, "to");
                if (text == null || text.isEmpty()) {
                    return;
                }
                if (user == null) {
                    sendNotice("请先发送 login 消息");
                    return;
                }
                if (to == null || to.trim().isEmpty()) {
                    sendNotice("没有指定收件人");
                    return;
                }
                ClientHandler target = ONLINE.get(to.trim());
                if (target == null) {
                    // 对方不在线，告诉发送方，避免"消息发出去了但没人收到"的困惑
                    sendNotice("「" + to.trim() + "」不在线，消息未送达");
                    log("转发失败: " + user + " → " + to.trim() + " (对方不在线)");
                    return;
                }
                target.sendMessage(user, text);
                log(user + " → " + to.trim() + " : " + text);
            }
        }

        /** 把消息转发给这个客户端（from 是发送者昵称） */
        void sendMessage(String from, String text) {
            sendLine("{\"type\":\"msg\",\"from\":\"" + jsonEscape(from)
                    + "\",\"text\":\"" + jsonEscape(text) + "\"}");
        }

        void sendNotice(String text) {
            sendLine("{\"type\":\"notice\",\"text\":\"" + jsonEscape(text) + "\"}");
        }

        private synchronized void sendLine(String line) {
            if (writer == null) {
                return;
            }
            try {
                writer.write(line);
                writer.write("\n");
                writer.flush();
            } catch (Exception e) {
                // 写失败说明连接已断，等 readLine 那边抛异常走 offline
            }
        }

        private void offline() {
            if (user != null) {
                ONLINE.remove(user, this);
                log("下线: " + user + "  当前在线 " + ONLINE.size() + " 人");
                user = null;
            }
            closeQuietly();
        }

        void closeQuietly() {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
                // 忽略
            }
        }
    }

    private static String onlineNames() {
        List<String> names = new ArrayList<>(ONLINE.keySet());
        if (names.isEmpty()) {
            return "无";
        }
        return String.join("、", names);
    }

    // ---------------- 极简 JSON 处理（只支持我们协议里的扁平字符串对象） ----------------

    /**
     * 从 JSON 里取一个字符串字段。
     * 自己写而不是引第三方库，是为了让服务端保持"单文件、无依赖、javac 就能编译"。
     */
    static String jsonGet(String json, String key) {
        String token = "\"" + key + "\"";
        int i = json.indexOf(token);
        if (i < 0) {
            return null;
        }
        i = json.indexOf(':', i + token.length());
        if (i < 0) {
            return null;
        }
        i++;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i >= json.length() || json.charAt(i) != '"') {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (i++; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\') {
                i++;
                if (i >= json.length()) {
                    break;
                }
                char next = json.charAt(i);
                switch (next) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'u':
                        if (i + 4 < json.length()) {
                            try {
                                sb.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                            } catch (NumberFormatException e) {
                                // 转义不合法就忽略
                            }
                            i += 4;
                        }
                        break;
                    default:
                        sb.append(next);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    static String jsonEscape(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
