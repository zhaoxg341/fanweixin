package cn.itcast.weixin.db;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.HashMap;
import java.util.Map;

/**
 * 联系人备注的读写。
 *
 * 备注会出现在通讯录、会话列表、聊天标题栏三个地方，
 * 而 ListView 的 getView() 每滑动一次就会调一次，直接查库太浪费，
 * 所以这里加了一层内存缓存：读走缓存，写同时落库 + 更新缓存。
 */
public class RemarkDao {

    /** 联系人昵称 → 备注。用昵称做键，因为会话列表里只有昵称没有联系人 id */
    private static final Map<String, String> CACHE = new HashMap<>();
    private static boolean loaded = false;

    private RemarkDao() {
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        Cursor cursor = null;
        try {
            cursor = WeixinDbHelper.get().getReadableDatabase().query(
                    WeixinDbHelper.TABLE_REMARK, new String[]{"contact_name", "remark"},
                    null, null, null, null, null);
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                String remark = cursor.getString(1);
                if (name != null && remark != null && !remark.isEmpty()) {
                    CACHE.put(name, remark);
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

    /** 取某个联系人的备注，没有备注返回 null */
    public static String getByName(String name) {
        if (name == null) {
            return null;
        }
        ensureLoaded();
        return CACHE.get(name);
    }

    /**
     * 显示用名字：有备注显示备注，没有就显示原昵称。
     * 所有展示联系人名字的地方统一走这个方法，改完备注就能一起生效。
     */
    public static String displayName(String name) {
        String remark = getByName(name);
        return (remark == null || remark.trim().isEmpty()) ? name : remark;
    }

    /** 保存备注；remark 传空字符串表示删除备注 */
    public static void save(int contactId, String contactName, String remark) {
        if (contactName == null || contactName.trim().isEmpty()) {
            return;
        }
        ensureLoaded();
        try {
            if (remark == null || remark.trim().isEmpty()) {
                WeixinDbHelper.get().getWritableDatabase().delete(
                        WeixinDbHelper.TABLE_REMARK, "contact_name = ?",
                        new String[]{contactName});
                CACHE.remove(contactName);
                return;
            }
            String value = remark.trim();
            // 先删旧记录再插入，保证同一个昵称只有一条备注（相当于 upsert）
            WeixinDbHelper.get().getWritableDatabase().delete(
                    WeixinDbHelper.TABLE_REMARK, "contact_name = ?",
                    new String[]{contactName});
            ContentValues values = new ContentValues();
            values.put("contact_id", contactId);
            values.put("contact_name", contactName);
            values.put("remark", value);
            WeixinDbHelper.get().getWritableDatabase()
                    .insert(WeixinDbHelper.TABLE_REMARK, null, values);
            CACHE.put(contactName, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
