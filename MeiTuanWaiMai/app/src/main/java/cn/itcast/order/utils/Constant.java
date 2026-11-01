package cn.itcast.order.utils;

/**
 * 全局常量：服务器地址等配置
 */
public class Constant {

    /**
     * 是否从 Tomcat 服务器获取数据。
     * false：直接读取 assets/shop.json（默认，离线即可运行，方便课堂演示）
     * true ：通过 HttpURLConnection 请求 SHOP_JSON_URL（对应教材 12.3 服务器数据准备）
     */
    public static final boolean USE_SERVER = false;

    /**
     * 服务器上 shop.json 的地址。
     * Android 模拟器访问本机 Tomcat 要用 10.0.2.2；
     * 真机调试请改成电脑的局域网 IP，例如 http://192.168.1.100:8080/order/shop.json
     */
    public static final String SHOP_JSON_URL = "http://10.0.2.2:8080/order/shop.json";
}
