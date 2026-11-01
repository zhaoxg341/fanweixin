package cn.itcast.weixin.db;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.itcast.weixin.bean.MomentBean;
import cn.itcast.weixin.utils.AppPrefs;
import cn.itcast.weixin.utils.WeixinDataUtils;

/**
 * 朋友圈的增删改查。
 *
 * 和聊天记录一样，所有方法都是静态的，调用方只关心「拿到 List<MomentBean>」。
 * 第一次打开朋友圈时，会把 assets/weixin.json 里的示例动态搬进数据库，
 * 之后不管是发动态、点赞还是评论，都只和数据库打交道，重启 App 内容还在。
 *
 * 点赞用 "@me" 占位而不是直接写昵称：这样用户改了微信昵称，
 * 之前点的赞仍然算在他头上，不需要批量改数据库。
 */
public class MomentDao {

    private static final String PREF = "weixin_moment";
    private static final String KEY_SEEDED = "seeded";
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);

    private MomentDao() {
    }

    /**
     * 首次进入朋友圈时导入示例数据。
     * 用一个标记位记录「导过了」，这样即使用户把动态全删了，也不会又冒出来。
     */
    public static void ensureSeed(Context context) {
        if (context == null) {
            return;
        }
        SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (sp.getBoolean(KEY_SEEDED, false)) {
            return;
        }
        if (count() > 0) {
            sp.edit().putBoolean(KEY_SEEDED, true).apply();
            return;
        }
        List<MomentBean> seeds =
                WeixinDataUtils.parseMomentList(WeixinDataUtils.loadWeixinJson(context));
        long now = System.currentTimeMillis();
        for (int i = 0; i < seeds.size(); i++) {
            MomentBean m = seeds.get(i);
            // assets 里的动态本身是「最新在前」，这里给一个递减的时间戳保持顺序
            m.setTs(now - i * 30L * 60 * 1000);
            m.setMine(false);
            insert(m);
        }
        sp.edit().putBoolean(KEY_SEEDED, true).apply();
    }

    /** 全部动态，按时间倒序；每条动态的评论都已经装好 */
    public static List<MomentBean> queryAll() {
        List<MomentBean> list = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = WeixinDbHelper.get().getReadableDatabase().query(
                    WeixinDbHelper.TABLE_MOMENT, null, null, null,
                    null, null, "ts DESC, _id DESC");
            while (cursor.moveToNext()) {
                list.add(readMoment(cursor));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(cursor);
        }
        loadComments(list);
        return list;
    }

    /** 一次把所有评论查出来再按 moment_id 分发，避免每条动态单独查一次数据库 */
    private static void loadComments(List<MomentBean> moments) {
        if (moments.isEmpty()) {
            return;
        }
        Cursor cursor = null;
        try {
            cursor = WeixinDbHelper.get().getReadableDatabase().query(
                    WeixinDbHelper.TABLE_MOMENT_COMMENT, null, null, null,
                    null, null, "ts ASC, _id ASC");
            while (cursor.moveToNext()) {
                int momentId = cursor.getInt(cursor.getColumnIndexOrThrow("moment_id"));
                MomentBean target = findById(moments, momentId);
                if (target == null) {
                    continue;
                }
                MomentBean.CommentBean c = new MomentBean.CommentBean();
                c.setId(cursor.getInt(cursor.getColumnIndexOrThrow("_id")));
                c.setUser(cursor.getString(cursor.getColumnIndexOrThrow("user")));
                c.setReplyTo(cursor.getString(cursor.getColumnIndexOrThrow("reply_to")));
                c.setText(cursor.getString(cursor.getColumnIndexOrThrow("text")));
                c.setMine(cursor.getInt(cursor.getColumnIndexOrThrow("mine")) == 1);
                target.getComments().add(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(cursor);
        }
    }

    private static MomentBean findById(List<MomentBean> list, int id) {
        for (MomentBean m : list) {
            if (m.getId() == id) {
                return m;
            }
        }
        return null;
    }

    /** 发布一条动态，返回数据库主键 */
    public static long insert(MomentBean m) {
        if (m == null) {
            return -1;
        }
        try {
            if (m.getTs() == 0) {
                m.setTs(System.currentTimeMillis());
            }
            if (m.getTime() == null || m.getTime().trim().isEmpty()) {
                m.setTime(formatTime(m.getTs()));
            }
            ContentValues values = new ContentValues();
            values.put("name", m.getName());
            values.put("avatar", m.getAvatar());
            values.put("content", m.getContent());
            values.put("images", toJson(m.getImages()));
            values.put("time", m.getTime());
            values.put("ts", m.getTs());
            values.put("mine", m.isMine() ? 1 : 0);
            values.put("likes", toJson(m.getLikes()));
            long id = WeixinDbHelper.get().getWritableDatabase()
                    .insert(WeixinDbHelper.TABLE_MOMENT, null, values);
            m.setId((int) id);

            // 示例动态自带评论，一起落库
            for (MomentBean.CommentBean c : m.getComments()) {
                insertComment(m.getId(), c.getUser(), c.getReplyTo(), c.getText(), false);
            }
            return id;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void deleteMoment(int momentId) {
        try {
            SQLiteDatabase db = WeixinDbHelper.get().getWritableDatabase();
            db.delete(WeixinDbHelper.TABLE_MOMENT_COMMENT, "moment_id = ?",
                    new String[]{String.valueOf(momentId)});
            db.delete(WeixinDbHelper.TABLE_MOMENT, "_id = ?",
                    new String[]{String.valueOf(momentId)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 点赞 / 取消点赞，返回操作后「我」是否处于已赞状态 */
    public static boolean toggleLike(int momentId) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = WeixinDbHelper.get().getWritableDatabase();
            cursor = db.query(WeixinDbHelper.TABLE_MOMENT, new String[]{"likes"},
                    "_id = ?", new String[]{String.valueOf(momentId)},
                    null, null, null);
            if (!cursor.moveToFirst()) {
                return false;
            }
            List<String> likes = parseList(cursor.getString(0));
            boolean liked;
            if (likes.contains(MomentBean.ME_TAG)) {
                likes.remove(MomentBean.ME_TAG);
                liked = false;
            } else {
                likes.add(MomentBean.ME_TAG);
                liked = true;
            }
            ContentValues values = new ContentValues();
            values.put("likes", toJson(likes));
            db.update(WeixinDbHelper.TABLE_MOMENT, values, "_id = ?",
                    new String[]{String.valueOf(momentId)});
            return liked;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(cursor);
        }
    }

    /**
     * 写一条评论。
     *
     * @param replyTo 回复谁；传 null 表示直接评论这条动态
     * @return 新评论主键
     */
    public static long addComment(int momentId, String user, String replyTo, String text) {
        return insertComment(momentId, user, replyTo, text, true);
    }

    private static long insertComment(int momentId, String user, String replyTo,
                                      String text, boolean mine) {
        if (text == null || text.trim().isEmpty()) {
            return -1;
        }
        try {
            ContentValues values = new ContentValues();
            values.put("moment_id", momentId);
            values.put("user", user == null ? AppPrefs.getMyNick() : user);
            values.put("reply_to", replyTo == null ? "" : replyTo);
            values.put("text", text.trim());
            values.put("ts", System.currentTimeMillis());
            values.put("mine", mine ? 1 : 0);
            return WeixinDbHelper.get().getWritableDatabase()
                    .insert(WeixinDbHelper.TABLE_MOMENT_COMMENT, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void deleteComment(int commentId) {
        try {
            WeixinDbHelper.get().getWritableDatabase().delete(
                    WeixinDbHelper.TABLE_MOMENT_COMMENT, "_id = ?",
                    new String[]{String.valueOf(commentId)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int count() {
        Cursor cursor = null;
        try {
            cursor = WeixinDbHelper.get().getReadableDatabase()
                    .rawQuery("SELECT COUNT(*) FROM " + WeixinDbHelper.TABLE_MOMENT, null);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(cursor);
        }
        return 0;
    }

    // ---------------- 工具 ----------------

    private static MomentBean readMoment(Cursor cursor) {
        MomentBean m = new MomentBean();
        m.setId(cursor.getInt(cursor.getColumnIndexOrThrow("_id")));
        m.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        m.setAvatar(cursor.getString(cursor.getColumnIndexOrThrow("avatar")));
        m.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
        m.setImages(parseList(cursor.getString(cursor.getColumnIndexOrThrow("images"))));
        m.setTime(cursor.getString(cursor.getColumnIndexOrThrow("time")));
        m.setTs(cursor.getLong(cursor.getColumnIndexOrThrow("ts")));
        m.setMine(cursor.getInt(cursor.getColumnIndexOrThrow("mine")) == 1);
        m.setLikes(parseList(cursor.getString(cursor.getColumnIndexOrThrow("likes"))));
        return m;
    }

    private static String toJson(List<String> list) {
        JSONArray array = new JSONArray();
        if (list != null) {
            for (String item : list) {
                array.put(item == null ? "" : item);
            }
        }
        return array.toString();
    }

    private static List<String> parseList(String json) {
        List<String> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return list;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i);
                if (value != null && !value.trim().isEmpty()) {
                    list.add(value);
                }
            }
        } catch (Exception e) {
            // 兼容早期用逗号拼接的写法
            for (String part : json.split(",")) {
                if (!part.trim().isEmpty()) {
                    list.add(part.trim());
                }
            }
        }
        return list;
    }

    /** 朋友圈里的时间：一天内说「几分钟前」，更久就显示日期 */
    public static String formatTime(long ts) {
        long diff = System.currentTimeMillis() - ts;
        if (diff < 60 * 1000L) {
            return "刚刚";
        }
        if (diff < 60 * 60 * 1000L) {
            return (diff / (60 * 1000L)) + "分钟前";
        }
        if (diff < 24 * 60 * 60 * 1000L) {
            return (diff / (60 * 60 * 1000L)) + "小时前";
        }
        return DATE_FMT.format(new Date(ts));
    }

    private static void closeQuietly(Cursor cursor) {
        if (cursor != null) {
            try {
                cursor.close();
            } catch (Exception ignored) {
            }
        }
    }
}
