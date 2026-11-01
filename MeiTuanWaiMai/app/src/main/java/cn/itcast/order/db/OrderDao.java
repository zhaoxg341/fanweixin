package cn.itcast.order.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import cn.itcast.order.bean.OrderBean;
import cn.itcast.order.bean.OrderItemBean;

/**
 * 订单表的增删改查。
 * 全部使用同步方法，调用方在界面线程直接调用即可（数据量小、单机数据库）。
 */
public class OrderDao {

    private final OrderDbHelper helper;

    public OrderDao(Context context) {
        helper = new OrderDbHelper(context.getApplicationContext());
    }

    /** 插入订单，返回自增主键 */
    public long insert(OrderBean order) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("order_no", order.getOrderNo());
        values.put("shop_id", order.getShopId());
        values.put("shop_name", order.getShopName());
        values.put("shop_banner", order.getShopBanner());
        values.put("goods_price", order.getGoodsPrice().toPlainString());
        values.put("distribution", order.getDistribution().toPlainString());
        values.put("discount", order.getDiscount().toPlainString());
        values.put("total", order.getTotal().toPlainString());
        values.put("address", order.getAddress());
        values.put("receiver", order.getReceiver());
        values.put("phone", order.getPhone());
        values.put("remark", order.getRemark());
        values.put("tableware", order.getTableware());
        values.put("arrive_time", order.getArriveTime());
        values.put("status", order.getStatus());
        values.put("create_time", order.getCreateTime());
        values.put("items_json", itemsToJson(order.getItems()));
        long id = db.insert(OrderDbHelper.TABLE, null, values);
        order.setId(id);
        return id;
    }

    /** 更新订单状态（支付成功、取消订单时调用） */
    public void updateStatus(long orderId, int status) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);
        db.update(OrderDbHelper.TABLE, values, "_id=?", new String[]{String.valueOf(orderId)});
    }

    public OrderBean queryById(long orderId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(OrderDbHelper.TABLE, null, "_id=?",
                new String[]{String.valueOf(orderId)}, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return readOrder(cursor);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    /** 全部订单，按下单时间倒序 */
    public List<OrderBean> queryAll() {
        return query("1=1", null);
    }

    /** 按状态查询；传 -1 表示全部 */
    public List<OrderBean> queryByStatus(int status) {
        if (status < 0) {
            return queryAll();
        }
        return query("status=?", new String[]{String.valueOf(status)});
    }

    public int countByStatus(int status) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + OrderDbHelper.TABLE + " WHERE status=?",
                new String[]{String.valueOf(status)});
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } finally {
            cursor.close();
        }
        return 0;
    }

    private List<OrderBean> query(String selection, String[] args) {
        List<OrderBean> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(OrderDbHelper.TABLE, null, selection, args,
                null, null, "create_time DESC");
        try {
            while (cursor.moveToNext()) {
                list.add(readOrder(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    private OrderBean readOrder(Cursor cursor) {
        OrderBean order = new OrderBean();
        order.setId(cursor.getLong(cursor.getColumnIndexOrThrow("_id")));
        order.setOrderNo(cursor.getString(cursor.getColumnIndexOrThrow("order_no")));
        order.setShopId(cursor.getInt(cursor.getColumnIndexOrThrow("shop_id")));
        order.setShopName(cursor.getString(cursor.getColumnIndexOrThrow("shop_name")));
        order.setShopBanner(cursor.getString(cursor.getColumnIndexOrThrow("shop_banner")));
        order.setGoodsPrice(decimal(cursor.getString(cursor.getColumnIndexOrThrow("goods_price"))));
        order.setDistribution(decimal(cursor.getString(cursor.getColumnIndexOrThrow("distribution"))));
        order.setDiscount(decimal(cursor.getString(cursor.getColumnIndexOrThrow("discount"))));
        order.setTotal(decimal(cursor.getString(cursor.getColumnIndexOrThrow("total"))));
        order.setAddress(cursor.getString(cursor.getColumnIndexOrThrow("address")));
        order.setReceiver(cursor.getString(cursor.getColumnIndexOrThrow("receiver")));
        order.setPhone(cursor.getString(cursor.getColumnIndexOrThrow("phone")));
        order.setRemark(cursor.getString(cursor.getColumnIndexOrThrow("remark")));
        order.setTableware(cursor.getInt(cursor.getColumnIndexOrThrow("tableware")));
        order.setArriveTime(cursor.getString(cursor.getColumnIndexOrThrow("arrive_time")));
        order.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow("status")));
        order.setCreateTime(cursor.getLong(cursor.getColumnIndexOrThrow("create_time")));
        order.setItems(jsonToItems(cursor.getString(cursor.getColumnIndexOrThrow("items_json"))));
        return order;
    }

    private BigDecimal decimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String itemsToJson(List<OrderItemBean> items) {
        JSONArray array = new JSONArray();
        try {
            for (OrderItemBean item : items) {
                JSONObject obj = new JSONObject();
                obj.put("foodId", item.getFoodId());
                obj.put("foodName", item.getFoodName());
                obj.put("foodPic", item.getFoodPic());
                obj.put("price", item.getPrice().toPlainString());
                obj.put("count", item.getCount());
                array.put(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return array.toString();
    }

    private List<OrderItemBean> jsonToItems(String json) {
        List<OrderItemBean> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return list;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                OrderItemBean item = new OrderItemBean();
                item.setFoodId(obj.optInt("foodId"));
                item.setFoodName(obj.optString("foodName"));
                item.setFoodPic(obj.optString("foodPic"));
                item.setPrice(decimal(obj.optString("price")));
                item.setCount(obj.optInt("count"));
                list.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
