package cn.itcast.milktea.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 用户偏好（SharedPreferences）：存"我的"页面的昵称、手机号、收货地址等小数据。
 * 只存少量键值对时用它；大量结构化数据（订单）用 SQLite。
 */
public class AppPrefs {

    private static final String FILE = "milktea_prefs";

    private static final String KEY_NICK = "nick";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_ADDRESS = "address";

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String getNick(Context context) {
        return prefs(context).getString(KEY_NICK, "奶茶星人");
    }

    public static void setNick(Context context, String nick) {
        prefs(context).edit().putString(KEY_NICK, nick).apply();
    }

    public static String getPhone(Context context) {
        return prefs(context).getString(KEY_PHONE, "138****8888");
    }

    public static void setPhone(Context context, String phone) {
        prefs(context).edit().putString(KEY_PHONE, phone).apply();
    }

    public static String getAddress(Context context) {
        return prefs(context).getString(KEY_ADDRESS, "大学城桂花园 6 栋 302");
    }

    public static void setAddress(Context context, String address) {
        prefs(context).edit().putString(KEY_ADDRESS, address).apply();
    }
}
