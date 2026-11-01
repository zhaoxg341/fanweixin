package cn.itcast.weixin.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import cn.itcast.weixin.WeixinApp;

/**
 * SQLite 数据库。六张表：
 *
 * t_message  聊天记录。每条消息一行，用 chat_id 归属到某个会话；
 *            type=0 是文字，type=1 是图片（content 存本机图片路径）。
 * t_remark   联系人备注。收到备注后，通讯录、会话列表、聊天标题都会跟着改。
 * t_chat     额外会话。原本会话列表来自 assets/weixin.json，
 *            但在线聊天时如果对方不在名单里，就会新建一个会话，这张表负责把它存下来。
 * t_avatar   好友自定义头像（相册导入的本地路径或内置头像名）。
 * t_moment   朋友圈动态。可以自己发，也可以是从 assets 导入的初始动态。
 * t_moment_comment  动态下面的评论/回复。
 * t_favorite 收藏。长按消息收藏后存这里，"我-收藏"页面读它。
 */
public class WeixinDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "weixin.db";
    /** 6：新增 t_favorite 收藏表 */
    private static final int DB_VERSION = 6;

    public static final String TABLE_MESSAGE = "t_message";
    public static final String TABLE_REMARK = "t_remark";
    public static final String TABLE_CHAT = "t_chat";
    public static final String TABLE_AVATAR = "t_avatar";
    public static final String TABLE_MOMENT = "t_moment";
    public static final String TABLE_MOMENT_COMMENT = "t_moment_comment";
    public static final String TABLE_FAVORITE = "t_favorite";

    private static WeixinDbHelper instance;

    private WeixinDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /** 全局单例，避免多个 Helper 各自持有连接 */
    public static synchronized WeixinDbHelper get() {
        if (instance == null) {
            instance = new WeixinDbHelper(WeixinApp.getContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 统一用 IF NOT EXISTS：这样 onUpgrade 里直接调 onCreate 也能安全地"只补新表、不动旧数据"
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGE + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "chat_id INTEGER NOT NULL,"
                + "content TEXT,"
                + "from_me INTEGER NOT NULL DEFAULT 0,"
                + "time TEXT,"
                + "ts INTEGER NOT NULL,"
                + "type INTEGER NOT NULL DEFAULT 0,"
                + "status INTEGER NOT NULL DEFAULT 0,"
                + "extra TEXT)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_message_chat ON " + TABLE_MESSAGE + "(chat_id, ts)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_REMARK + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "contact_id INTEGER,"
                + "contact_name TEXT,"
                + "remark TEXT)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_remark_name ON " + TABLE_REMARK + "(contact_name)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CHAT + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "chat_id INTEGER NOT NULL,"
                + "name TEXT,"
                + "avatar TEXT,"
                + "last_msg TEXT,"
                + "time TEXT,"
                + "ts INTEGER NOT NULL,"
                + "is_group INTEGER NOT NULL DEFAULT 0)");

        // 好友自定义头像：一个昵称最多一条记录，没记录就表示用 assets 里的默认头像
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AVATAR + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "contact_name TEXT,"
                + "avatar TEXT)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_avatar_name ON " + TABLE_AVATAR + "(contact_name)");

        // 朋友圈动态。images / likes 直接存 JSON 数组字符串，字段不多，没必要再拆表。
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MOMENT + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT,"
                + "avatar TEXT,"
                + "content TEXT,"
                + "images TEXT,"
                + "time TEXT,"
                + "ts INTEGER NOT NULL,"
                + "mine INTEGER NOT NULL DEFAULT 0,"
                + "likes TEXT)");

        // 评论：reply_to 非空表示「回复某人」，界面显示成「我 回复 张三：…」
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MOMENT_COMMENT + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "moment_id INTEGER NOT NULL,"
                + "user TEXT,"
                + "reply_to TEXT,"
                + "text TEXT,"
                + "ts INTEGER NOT NULL,"
                + "mine INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_moment_comment ON "
                + TABLE_MOMENT_COMMENT + "(moment_id, ts)");

        // 收藏：长按聊天消息收藏。type 与 t_message 一致，content 原样存（红包/转账还是 JSON）
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FAVORITE + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "chat_id INTEGER NOT NULL,"
                + "from_name TEXT,"
                + "from_me INTEGER NOT NULL DEFAULT 0,"
                + "type INTEGER NOT NULL DEFAULT 0,"
                + "content TEXT,"
                + "time TEXT,"
                + "ts INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_favorite_ts ON "
                + TABLE_FAVORITE + "(ts)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 每次升级都先补新表（全部 IF NOT EXISTS），聊天记录、备注、朋友圈内容都保留
        onCreate(db);

        // v3 的 t_message 没有 type 字段，老库要手动补一列；
        // 新装的库在 onCreate 里已经带上，ALTER 会报「列已存在」，吞掉就行
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_MESSAGE
                        + " ADD COLUMN type INTEGER NOT NULL DEFAULT 0");
            } catch (Exception ignored) {
                // 列已存在，说明这张表是新版本新建的
            }
        }
        // v5：撤回状态 + 附加信息
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_MESSAGE
                        + " ADD COLUMN status INTEGER NOT NULL DEFAULT 0");
            } catch (Exception ignored) {
            }
            try {
                db.execSQL("ALTER TABLE " + TABLE_MESSAGE + " ADD COLUMN extra TEXT");
            } catch (Exception ignored) {
            }
        }
    }
}
