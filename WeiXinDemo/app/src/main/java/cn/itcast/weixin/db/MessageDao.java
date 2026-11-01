package cn.itcast.weixin.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.weixin.bean.MsgBean;

/**
 * 聊天记录的增删查。所有方法都是静态的，调用方不需要管理数据库连接。
 */
public class MessageDao {

    private MessageDao() {
    }

    /** 按会话取出全部消息，按时间正序 */
    public static List<MsgBean> query(int chatId) {
        List<MsgBean> list = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = WeixinDbHelper.get().getReadableDatabase().query(
                    WeixinDbHelper.TABLE_MESSAGE, null,
                    "chat_id = ?", new String[]{String.valueOf(chatId)},
                    null, null, "ts ASC, _id ASC");
            while (cursor.moveToNext()) {
                MsgBean msg = new MsgBean();
                msg.setId(cursor.getInt(cursor.getColumnIndexOrThrow("_id")));
                msg.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
                msg.setFromMe(cursor.getInt(cursor.getColumnIndexOrThrow("from_me")) == 1);
                msg.setTime(cursor.getString(cursor.getColumnIndexOrThrow("time")));
                msg.setTs(cursor.getLong(cursor.getColumnIndexOrThrow("ts")));
                msg.setType(cursor.getInt(cursor.getColumnIndexOrThrow("type")));
                msg.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow("status")));
                msg.setExtra(cursor.getString(cursor.getColumnIndexOrThrow("extra")));
                list.add(msg);
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

    /** 插入一条消息，插入后把自增主键回填到 MsgBean 上 */
    public static void insert(int chatId, MsgBean msg) {
        if (msg == null) {
            return;
        }
        try {
            if (msg.getTs() == 0) {
                msg.setTs(System.currentTimeMillis());
            }
            ContentValues values = new ContentValues();
            values.put("chat_id", chatId);
            values.put("content", msg.getContent());
            values.put("from_me", msg.isFromMe() ? 1 : 0);
            values.put("time", msg.getTime());
            values.put("ts", msg.getTs());
            values.put("type", msg.getType());
            values.put("status", msg.getStatus());
            values.put("extra", msg.getExtra());
            long id = WeixinDbHelper.get().getWritableDatabase()
                    .insert(WeixinDbHelper.TABLE_MESSAGE, null, values);
            msg.setId((int) id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int count(int chatId) {
        Cursor cursor = null;
        try {
            cursor = WeixinDbHelper.get().getReadableDatabase().rawQuery(
                    "SELECT COUNT(*) FROM " + WeixinDbHelper.TABLE_MESSAGE + " WHERE chat_id = ?",
                    new String[]{String.valueOf(chatId)});
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

    /** 标记一条消息为已撤回 */
    public static void markRecalled(int msgId) {
        ContentValues values = new ContentValues();
        values.put("status", 1);
        try {
            WeixinDbHelper.get().getWritableDatabase()
                    .update(WeixinDbHelper.TABLE_MESSAGE, values, "_id = ?",
                            new String[]{String.valueOf(msgId)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 更新一条消息的 content（红包被领取时用） */
    public static void updateContent(int msgId, String content) {
        ContentValues values = new ContentValues();
        values.put("content", content);
        try {
            WeixinDbHelper.get().getWritableDatabase()
                    .update(WeixinDbHelper.TABLE_MESSAGE, values, "_id = ?",
                            new String[]{String.valueOf(msgId)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 删除单条消息（长按菜单里的「删除」） */
    public static void deleteById(int msgId) {
        try {
            WeixinDbHelper.get().getWritableDatabase()
                    .delete(WeixinDbHelper.TABLE_MESSAGE, "_id = ?",
                            new String[]{String.valueOf(msgId)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 清空某个会话的聊天记录 */
    public static void deleteByChat(int chatId) {
        try {
            SQLiteDatabase db = WeixinDbHelper.get().getWritableDatabase();
            db.delete(WeixinDbHelper.TABLE_MESSAGE, "chat_id = ?",
                    new String[]{String.valueOf(chatId)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
