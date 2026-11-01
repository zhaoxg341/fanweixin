package cn.itcast.weixin.utils;

/**
 * 全局常量
 */
public class Constant {

    /**
     * 是否从 Tomcat 服务器获取数据。
     * false：直接读取 assets/weixin.json（默认，离线即可运行）
     * true ：通过 HttpURLConnection 请求 WEIXIN_JSON_URL
     */
    public static final boolean USE_SERVER = false;

    /**
     * 服务器上 weixin.json 的地址。
     * 模拟器访问本机 Tomcat 用 10.0.2.2；真机请改成电脑局域网 IP。
     */
    public static final String WEIXIN_JSON_URL = "http://10.0.2.2:8080/weixin/weixin.json";

    /** 模拟对方自动回复的延迟（毫秒），0 表示不启用 */
    public static final long AUTO_REPLY_DELAY = 900L;
}
