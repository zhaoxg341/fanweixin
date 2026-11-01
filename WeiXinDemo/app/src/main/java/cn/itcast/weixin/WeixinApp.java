package cn.itcast.weixin;

import android.app.Application;
import android.content.Context;

import cn.itcast.weixin.utils.WeixinDataUtils;

/**
 * 全局 Application。
 *
 * 两个作用：
 * 1. 保存一份 applicationContext，供 SharedPreferences / SQLite 这类需要在
 *    非 Activity 环境里拿 Context 的工具类使用（避免到处传 Context 或内存泄漏）；
 * 2. 启动时预热一次 assets/weixin.json，把「本机默认用户」缓存到内存里。
 */
public class WeixinApp extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        WeixinDataUtils.warmUp(appContext);
    }

    public static Context getContext() {
        return appContext;
    }
}
