package cn.itcast.weixin.db;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.weixin.bean.FavoriteBean;

/**
 * 收藏表 t_favorite 的增删查。
 * 收藏的删除只影响收藏页，不会动原聊天记录。
 */
public class FavoriteDao {

    private FavoriteDao() {
    }

    /** 收藏一条消息 */
    public static void insert(FavoriteBean fav) {
        if (fav == null) {
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put("chat_id", fav.getChatId());
            values.put("from_name", fav.getFromName());
            values.put("from_me", fav.isFromMe() ? 1 : 0);
            values.put("type", fav.getType());
            values.put("content", fav.getContent());
            values.put("time", fav.getTime());
            values.put("ts", fav.getTs() == 0 ? System.currentTimeMillis() : fav.getTs());
            WeixinDbHelper.get().getWritableDatabase()
                    .insert(WeixinDbHelper.TABLE_FAVORITE, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 全部收藏，新收藏的排在前面 */
    public static List<FavoriteBean> queryAll() {
        List<FavoriteBean> list = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = WeixinDbHelper.get().getReadableDatabase().query(
                    WeixinDbHelper.TABLE_FAVORITE, null,
                    null, null, null, null, "ts DESC, _id DESC");
            while (cursor.moveToNext()) {
                FavoriteBean fav = new FavoriteBean();
                fav.setId(cursor.getInt(cursor.getColumnIndexOrThrow("_id")));
                fav.setChatId(cursor.getInt(cursor.getColumnIndexOrThrow("chat_id")));
                fav.setFromName(cursor.getString(cursor.getColumnIndexOrThrow("from_name")));
                fav.setFromMe(cursor.getInt(cursor.getColumnIndexOrThrow("from_me")) == 1);
                fav.setType(cursor.getInt(cursor.getColumnIndexOrThrow("type")));
                fav.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
                fav.setTime(cursor.getString(cursor.getColumnIndexOrThrow("time")));
                fav.setTs(cursor.getLong(cursor.getColumnIndexOrThrow("ts")));
                list.add(fav);
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

    /** 删除一条收藏 */
    public static void deleteById(int id) {
        try {
            WeixinDbHelper.get().getWritableDatabase()
                    .delete(WeixinDbHelper.TABLE_FAVORITE, "_id = ?",
                            new String[]{String.valueOf(id)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
