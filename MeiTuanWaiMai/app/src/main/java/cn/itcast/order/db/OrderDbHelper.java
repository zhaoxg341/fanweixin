package cn.itcast.order.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 订单数据库。
 * 商品明细直接以 JSON 字符串存在 items_json 列中：
 * 订单一旦生成就不会再改商品，没必要拆成两张表，读起来更简单。
 */
public class OrderDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "meituan_order.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE = "t_order";

    public OrderDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "order_no TEXT,"
                + "shop_id INTEGER,"
                + "shop_name TEXT,"
                + "shop_banner TEXT,"
                + "goods_price TEXT,"
                + "distribution TEXT,"
                + "discount TEXT,"
                + "total TEXT,"
                + "address TEXT,"
                + "receiver TEXT,"
                + "phone TEXT,"
                + "remark TEXT,"
                + "tableware INTEGER,"
                + "arrive_time TEXT,"
                + "status INTEGER,"
                + "create_time INTEGER,"
                + "items_json TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }
}
