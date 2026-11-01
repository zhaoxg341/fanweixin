package cn.itcast.weixin.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.weixin.bean.ChatBean;

/**
 * 额外会话的持久化。
 *
 * 会话列表默认来自 assets/weixin.json（那 8 个是写死的演示会话）。
 * 在线聊天时对方可能不在这个名单里，于是会新建会话 —— 这个新会话就存在 t_chat 表，
 * 下次启动时和 JSON 里的会话合并，避免"聊完重启就消失"。
 */
public class ExtraChatDao {

    private ExtraChatDao() {
    }

    public static List<ChatBean> queryAll() {
        List<ChatBean> list = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = WeixinDbHelper.get().getReadableDatabase().query(
                    WeixinDbHelper.TABLE_CHAT, null, null, null, null, null, "ts ASC");
            while (cursor.moveToNext()) {
                ChatBean chat = new ChatBean();
                chat.setId(cursor.getInt(cursor.getColumnIndexOrThrow("chat_id")));
                chat.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                chat.setAvatar(cursor.getString(cursor.getColumnIndexOrThrow("avatar")));
                chat.setLastMsg(cursor.getString(cursor.getColumnIndexOrThrow("last_msg")));
                chat.setTime(cursor.getString(cursor.getColumnIndexOrThrow("time")));
                chat.setGroup(cursor.getInt(cursor.getColumnIndexOrThrow("is_group")) == 1);
                chat.setUnread(0);
                list.add(chat);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    /** 新增或更新一个会话（按 chat_id 去重） */
    public static void upsert(ChatBean chat) {
        if (chat == null) {
            return;
        }
        try {
            SQLiteDatabase db = WeixinDbHelper.get().getWritableDatabase();
            db.delete(WeixinDbHelper.TABLE_CHAT, "chat_id = ?",
                    new String[]{String.valueOf(chat.getId())});
            ContentValues values = new ContentValues();
            values.put("chat_id", chat.getId());
            values.put("name", chat.getName());
            values.put("avatar", chat.getAvatar());
            values.put("last_msg", chat.getLastMsg());
            values.put("time", chat.getTime());
            values.put("ts", System.currentTimeMillis());
            values.put("is_group", chat.isGroup() ? 1 : 0);
            db.insert(WeixinDbHelper.TABLE_CHAT, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 库里已有的最大 chat_id，用来决定新会话该分配哪个 id */
    public static int maxChatId() {
        Cursor cursor = null;
        try {
            cursor = WeixinDbHelper.get().getReadableDatabase().rawQuery(
                    "SELECT MAX(chat_id) FROM " + WeixinDbHelper.TABLE_CHAT, null);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }
}
