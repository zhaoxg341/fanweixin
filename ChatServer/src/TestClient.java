import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * ChatServer 的联调测试客户端（不参与 App，仅用于本机验证服务端转发是否正常）。
 *
 * 用法：
 *   java TestClient 127.0.0.1 8888 张三 李思思
 *
 * 它会模拟两台设备：张三 和 李思思 分别登录，
 * 然后由张三发一条消息给李思思，最后打印双方收到的原始数据。
 * 如果李思思那边打印出 {"type":"msg","from":"张三",...}，说明整条链路通了。
 */
public class TestClient {

    /** 模拟一台设备：登录并接收消息，收到的每一行都打印出来 */
    static class Device implements Runnable {

        private final String host;
        private final int port;
        private final String nick;

        Device(String host, int port, String nick) {
            this.host = host;
            this.port = port;
            this.nick = nick;
        }

        private BufferedWriter writer;

        void send(String json) throws Exception {
            writer.write(json);
            writer.write("\n");
            writer.flush();
        }

        @Override
        public void run() {
            try {
                Socket socket = new Socket(host, port);
                socket.setSoTimeout(1000);
                writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), "UTF-8"));

                send("{\"type\":\"login\",\"from\":\"" + nick + "\",\"text\":\"\"}");
                System.out.println("[" + nick + "] 已登录");

                long deadline = System.currentTimeMillis() + 6000;
                while (System.currentTimeMillis() < deadline) {
                    try {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        System.out.println("[" + nick + "] 收到 <- " + line);
                    } catch (SocketTimeoutException ignored) {
                        // 超时继续等，直到总时限
                    }
                }
                socket.close();
            } catch (Exception e) {
                System.out.println("[" + nick + "] 出错: " + e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("用法: java TestClient <host> <port> <昵称A> <昵称B>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String nickA = args[2];
        String nickB = args[3];

        Device a = new Device(host, port, nickA);
        Device b = new Device(host, port, nickB);
        new Thread(a).start();
        Thread.sleep(500);
        new Thread(b).start();

        // 等两边都完成 login
        Thread.sleep(1000);

        // 张三 发给 李思思
        System.out.println();
        System.out.println(">>> 由 " + nickA + " 发消息给 " + nickB + "：你好，我是" + nickA);
        a.send("{\"type\":\"msg\",\"from\":\"" + nickA + "\",\"to\":\"" + nickB
                + "\",\"text\":\"你好，我是" + nickA + "\"}");

        // 再试一条收件人不在线的，验证错误提示
        Thread.sleep(800);
        System.out.println(">>> 由 " + nickA + " 发消息给一个不存在的用户：测试不在线提示");
        a.send("{\"type\":\"msg\",\"from\":\"" + nickA + "\",\"to\":\"不存在的人\",\"text\":\"在吗\"}");

        Thread.sleep(3000);
        System.out.println();
        System.out.println("测试结束。");
    }
}
