package cn.itcast.weixin.db;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.HashMap;
import java.util.Map;

import cn.itcast.weixin.utils.AvatarStore;

/**
 * 好友自定义头像的读写。
 *
 * 和 RemarkDao 一个套路：
 * 1. 用"联系人昵称"做键（会话列表里只有昵称，没有联系人 id）；
 * 2. 读走内存缓存，写同时落库 + 更新缓存，避免 ListView 每次 getView 都查库；
 * 3. 所有展示头像的地方统一调 displayAvatar()，改完头像各处一起生效。
 *
 * 没有记录 = 用 assets/weixin.json 里原本的默认头像，所以"恢复默认"只要删掉记录即可。
 */
public class AvatarDao {

    /** 可选头像池：和 res/drawable-nodpi 下的文件一一对应 */
    public static final String[] POOL = {
            "avatar1", "avatar2", "avatar3", "avatar4",
            "avatar5", "avatar6", "avatar7", "avatar8",
            "avatar9", "avatar10", "avatar11", "avatar12",
            "avatar13", "avatar14", "avatar15", "avatar16"
    };

    /** 联系人昵称 → 头像资源名 */
    private static final Map<String, String> CACHE = new HashMap<>();
    private static boolean loaded = false;

    private AvatarDao() {
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        Cursor cursor = null;
        try {
            cursor = WeixinDbHelper.get().getReadableDatabase().query(
                    WeixinDbHelper.TABLE_AVATAR, new String[]{"contact_name", "avatar"},
                    null, null, null, null, null);
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                String avatar = cursor.getString(1);
                if (name != null && avatar != null && !avatar.isEmpty()) {
                    CACHE.put(name, normalize(avatar));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        loaded = true;
    }

    /**
     * JSON 里可能写成 avatar1.jpg，资源名不能带点，统一去掉扩展名。
     * 但相册选的头像存的是绝对路径（/data/.../xxx.jpg），路径里的点不能乱截，
     * 所以开头是 "/" 的一律原样返回。
     */
    public static String normalize(String avatar) {
        if (avatar == null) {
            return null;
        }
        String key = avatar.trim();
        if (key.startsWith("/")) {
            return key;
        }
        int dot = key.lastIndexOf('.');
        if (dot > 0) {
            key = key.substring(0, dot);
        }
        return key;
    }

    /** 取自定义头像，没设置过返回 null */
    public static String getByName(String name) {
        if (name == null) {
            return null;
        }
        ensureLoaded();
        return CACHE.get(name);
    }

    /**
     * 显示用头像：设置过就用自己的，没设置就用 fallback（assets 里的原头像）。
     * 所有展示好友头像的地方统一走这个方法。
     */
    public static String displayAvatar(String name, String fallback) {
        String custom = getByName(name);
        return (custom == null || custom.trim().isEmpty()) ? fallback : custom;
    }

    /** 保存头像；avatar 传 null 或空字符串表示恢复默认（删除记录） */
    public static void save(String contactName, String avatar) {
        if (contactName == null || contactName.trim().isEmpty()) {
            return;
        }
        ensureLoaded();
        try {
            String old = CACHE.get(contactName);
            String key = normalize(avatar);
            if (key == null || key.isEmpty()) {
                WeixinDbHelper.get().getWritableDatabase().delete(
                        WeixinDbHelper.TABLE_AVATAR, "contact_name = ?",
                        new String[]{contactName});
                CACHE.remove(contactName);
                // 恢复默认时把之前从相册导入的图片也删掉
                AvatarStore.deleteIfLocal(old);
                return;
            }
            // 先删旧的再插入，保证一个昵称只有一条记录（upsert）
            WeixinDbHelper.get().getWritableDatabase().delete(
                    WeixinDbHelper.TABLE_AVATAR, "contact_name = ?",
                    new String[]{contactName});
            ContentValues values = new ContentValues();
            values.put("contact_name", contactName);
            values.put("avatar", key);
            WeixinDbHelper.get().getWritableDatabase()
                    .insert(WeixinDbHelper.TABLE_AVATAR, null, values);
            CACHE.put(contactName, key);
            // 换了新图之后，上一张旧图就没用了
            if (old != null && !old.equals(key)) {
                AvatarStore.deleteIfLocal(old);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
