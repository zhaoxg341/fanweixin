package cn.itcast.weixin.utils;

import android.content.Context;
import android.content.SharedPreferences;

import cn.itcast.weixin.WeixinApp;
import cn.itcast.weixin.bean.UserBean;

/**
 * 轻量设置存储（SharedPreferences）。
 *
 * 存的是「会变、但数据量很小」的东西：我的昵称、我的头像、服务器地址端口、是否已登录。
 * 聊天记录这种会不断增长的数据放 SQLite（见 MessageDao）。
 *
 * 这里的 getMyNick() / getMyAvatar() 做了回落处理：
 * 用户没自己改过，就返回 assets/weixin.json 里的默认值，
 * 这样所有调用方都只需要问 AppPrefs 一句就行。
 */
public class AppPrefs {

    private static final String FILE = "weixin_prefs";
    private static final String KEY_NICK = "my_nick";
    private static final String KEY_AVATAR = "my_avatar";
    private static final String KEY_HOST = "server_host";
    private static final String KEY_PORT = "server_port";
    private static final String KEY_LOGGED = "logged_in";
    private static final String KEY_LAN_MODE = "lan_mode";
    private static final String KEY_WXID = "my_wxid";
    private static final String KEY_GENDER = "my_gender";
    private static final String KEY_REGION = "my_region";

    /** 默认服务器端口，与 ChatServer 里保持一致 */
    public static final int DEFAULT_PORT = 8888;

    private AppPrefs() {
    }

    private static SharedPreferences sp() {
        return WeixinApp.getContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // ---------------- 我的昵称 / 头像 ----------------

    /** 我的昵称；没设置过就用 JSON 里的默认昵称 */
    public static String getMyNick() {
        String nick = sp().getString(KEY_NICK, "");
        if (nick != null && !nick.trim().isEmpty()) {
            return nick;
        }
        UserBean user = WeixinDataUtils.getLocalUser();
        if (user != null && user.getNickName() != null && !user.getNickName().isEmpty()) {
            return user.getNickName();
        }
        return "我";
    }

    public static void setMyNick(String nick) {
        sp().edit().putString(KEY_NICK, nick).apply();
    }

    /** 我的头像图片名；没设置过就用 JSON 里的默认头像 */
    public static String getMyAvatar() {
        String avatar = sp().getString(KEY_AVATAR, "");
        if (avatar != null && !avatar.trim().isEmpty()) {
            return avatar;
        }
        UserBean user = WeixinDataUtils.getLocalUser();
        if (user != null && user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            return user.getAvatar();
        }
        return "avatar_me.jpg";
    }

    public static void setMyAvatar(String avatar) {
        sp().edit().putString(KEY_AVATAR, avatar).apply();
    }

    // ---------------- 我的微信号 / 性别 / 地区（设置页可改） ----------------

    /** 微信号；没改过就用 JSON 里的默认微信号 */
    public static String getMyWxId() {
        String wxid = sp().getString(KEY_WXID, "");
        if (wxid != null && !wxid.trim().isEmpty()) {
            return wxid;
        }
        UserBean user = WeixinDataUtils.getLocalUser();
        if (user != null && user.getWeixinId() != null && !user.getWeixinId().isEmpty()) {
            return user.getWeixinId();
        }
        return "wxid_demo";
    }

    public static void setMyWxId(String wxid) {
        sp().edit().putString(KEY_WXID, wxid).apply();
    }

    /** 性别：未设置时返回空串，界面显示"未设置" */
    public static String getMyGender() {
        return sp().getString(KEY_GENDER, "");
    }

    public static void setMyGender(String gender) {
        sp().edit().putString(KEY_GENDER, gender).apply();
    }

    public static String getMyRegion() {
        return sp().getString(KEY_REGION, "");
    }

    public static void setMyRegion(String region) {
        sp().edit().putString(KEY_REGION, region).apply();
    }

    // ---------------- 服务器地址 ----------------

    public static String getServerHost() {
        return sp().getString(KEY_HOST, "");
    }

    public static void setServerHost(String host) {
        sp().edit().putString(KEY_HOST, host).apply();
    }

    public static int getServerPort() {
        return sp().getInt(KEY_PORT, DEFAULT_PORT);
    }

    public static void setServerPort(int port) {
        sp().edit().putInt(KEY_PORT, port).apply();
    }

    // ---------------- 登录状态与运行模式 ----------------

    public static boolean isLoggedIn() {
        return sp().getBoolean(KEY_LOGGED, false);
    }

    public static void setLoggedIn(boolean loggedIn) {
        sp().edit().putBoolean(KEY_LOGGED, loggedIn).apply();
    }

    /**
     * true  = 局域网在线模式，消息经 ChatServer 转发给另一台设备；
     * false = 本地演示模式，就是原来那套读 assets/weixin.json + 模拟回复，功能完全保留。
     */
    public static boolean isLanMode() {
        return sp().getBoolean(KEY_LAN_MODE, false);
    }

    public static void setLanMode(boolean lanMode) {
        sp().edit().putBoolean(KEY_LAN_MODE, lanMode).apply();
    }

    /** 退出登录：清掉登录态，但保留昵称和服务器地址方便下次自动填 */
    public static void logout() {
        sp().edit().putBoolean(KEY_LOGGED, false).apply();
    }
}
