package cn.itcast.milktea.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import cn.itcast.milktea.bean.OrderBean;
import cn.itcast.milktea.bean.OrderItemBean;

/**
 * 订单 DAO：负责 t_order / t_order_item 两张表的读写。
 *
 * 要点：
 *  1. insertOrder 用事务把"主表 + 明细表"包成一个整体，要么都成功要么都不写，
 *     避免出现"有订单但没有明细"的脏数据；
 *  2. 金额在数据库里按 TEXT 存 BigDecimal.toPlainString()，读出来再转回 BigDecimal，
 *     避免 SQLite 没有小数类型造成的精度问题；
 *  3. 查询统一按 created_at 倒序（最新的订单排在最上面）。
 */
public class OrderDao {

    private final OrderDbHelper helper;

    public OrderDao(Context context) {
        helper = OrderDbHelper.get(context);
    }

    /** 新建订单（含明细），返回自增 id；失败返回 -1 */
    public long insertOrder(OrderBean order) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("order_no", order.getOrderNo());
            values.put("created_at", order.getCreatedAt());
            values.put("goods_total", order.getGoodsTotal().toPlainString());
            values.put("packing_fee", order.getPackingFee().toPlainString());
            values.put("delivery_fee", order.getDeliveryFee().toPlainString());
            values.put("discount", order.getDiscount().toPlainString());
            values.put("total", order.getTotal().toPlainString());
            values.put("status", order.getStatus());
            values.put("mode", order.getMode());
            values.put("pickup_code", order.getPickupCode());
            values.put("address", order.getAddress());
            values.put("remark", order.getRemark());
            long id = db.insert(OrderDbHelper.TABLE_ORDER, null, values);

            for (OrderItemBean item : order.getItems()) {
                ContentValues itemValues = new ContentValues();
                itemValues.put("order_no", order.getOrderNo());
                itemValues.put("goods_id", item.getGoodsId());
                itemValues.put("name", item.getName());
                itemValues.put("spec_text", item.getSpecText());
                itemValues.put("unit_price", item.getUnitPrice().toPlainString());
                itemValues.put("count", item.getCount());
                db.insert(OrderDbHelper.TABLE_ITEM, null, itemValues);
            }
            db.setTransactionSuccessful();
            return id;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    /** 所有订单，最新的在最前 */
    public List<OrderBean> queryAll() {
        return query("SELECT * FROM " + OrderDbHelper.TABLE_ORDER
                + " ORDER BY created_at DESC");
    }

    /** 按订单号查单个订单 */
    public OrderBean queryByOrderNo(String orderNo) {
        List<OrderBean> list = query("SELECT * FROM " + OrderDbHelper.TABLE_ORDER
                + " WHERE order_no = ? ", new String[]{orderNo});
        return list.isEmpty() ? null : list.get(0);
    }

    /** 更新订单状态（去支付 / 确认取餐 / 取消） */
    public int updateStatus(String orderNo, int status) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        return helper.getWritableDatabase().update(OrderDbHelper.TABLE_ORDER, values,
                "order_no = ?", new String[]{orderNo});
    }

    private List<OrderBean> query(String sql, String... args) {
        List<OrderBean> orders = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = helper.getReadableDatabase().rawQuery(sql, args);
            while (cursor.moveToNext()) {
                OrderBean order = new OrderBean();
                order.setId(cursor.getLong(cursor.getColumnIndex("_id")));
                order.setOrderNo(cursor.getString(cursor.getColumnIndex("order_no")));
                order.setCreatedAt(cursor.getLong(cursor.getColumnIndex("created_at")));
                order.setGoodsTotal(new BigDecimal(cursor.getString(cursor.getColumnIndex("goods_total"))));
                order.setPackingFee(new BigDecimal(cursor.getString(cursor.getColumnIndex("packing_fee"))));
                order.setDeliveryFee(new BigDecimal(cursor.getString(cursor.getColumnIndex("delivery_fee"))));
                order.setDiscount(new BigDecimal(cursor.getString(cursor.getColumnIndex("discount"))));
                order.setTotal(new BigDecimal(cursor.getString(cursor.getColumnIndex("total"))));
                order.setStatus(cursor.getInt(cursor.getColumnIndex("status")));
                order.setMode(cursor.getInt(cursor.getColumnIndex("mode")));
                order.setPickupCode(cursor.getString(cursor.getColumnIndex("pickup_code")));
                order.setAddress(cursor.getString(cursor.getColumnIndex("address")));
                order.setRemark(cursor.getString(cursor.getColumnIndex("remark")));
                order.getItems().addAll(queryItems(order.getOrderNo()));
                orders.add(order);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return orders;
    }

    /** 查某个订单的全部明细 */
    private List<OrderItemBean> queryItems(String orderNo) {
        List<OrderItemBean> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = helper.getReadableDatabase().rawQuery(
                    "SELECT * FROM " + OrderDbHelper.TABLE_ITEM + " WHERE order_no = ?",
                    new String[]{orderNo});
            while (cursor.moveToNext()) {
                items.add(new OrderItemBean(
                        cursor.getInt(cursor.getColumnIndex("goods_id")),
                        cursor.getString(cursor.getColumnIndex("name")),
                        cursor.getString(cursor.getColumnIndex("spec_text")),
                        new BigDecimal(cursor.getString(cursor.getColumnIndex("unit_price"))),
                        cursor.getInt(cursor.getColumnIndex("count"))));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return items;
    }
}
