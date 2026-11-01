package cn.itcast.milktea.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 订单数据库（SQLiteOpenHelper 标准写法）。
 *
 * 两张表：
 *   t_order      订单主表：订单号 / 时间 / 金额 / 状态 / 取餐码…
 *   t_order_item 订单明细：一行 = 订单里的一个商品
 * 明细表通过 order_no 关联主表（一对多）。
 *
 * 单例 get()：OpenHelper 本身线程安全开销较大，整个 App 共用一个实例即可。
 */
public class OrderDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "milktea.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_ORDER = "t_order";
    public static final String TABLE_ITEM = "t_order_item";

    private static OrderDbHelper instance;

    public static synchronized OrderDbHelper get(Context context) {
        if (instance == null) {
            instance = new OrderDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private OrderDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_ORDER + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "order_no TEXT NOT NULL,"
                + "created_at INTEGER NOT NULL,"
                + "goods_total TEXT,"
                + "packing_fee TEXT,"
                + "delivery_fee TEXT,"
                + "discount TEXT,"
                + "total TEXT,"
                + "status INTEGER NOT NULL DEFAULT 0,"
                + "mode INTEGER NOT NULL DEFAULT 0,"
                + "pickup_code TEXT,"
                + "address TEXT,"
                + "remark TEXT)");
        db.execSQL("CREATE INDEX idx_order_no ON " + TABLE_ORDER + "(order_no)");

        db.execSQL("CREATE TABLE " + TABLE_ITEM + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "order_no TEXT NOT NULL,"
                + "goods_id INTEGER NOT NULL DEFAULT 0,"
                + "name TEXT,"
                + "spec_text TEXT,"
                + "unit_price TEXT,"
                + "count INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE INDEX idx_item_no ON " + TABLE_ITEM + "(order_no)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 课设阶段直接重建，不做数据迁移
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEM);
        onCreate(db);
    }
}
