package cn.itcast.milktea.bean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单。
 * 状态：0 待支付 → 1 制作中 → 2 已完成；3 已取消。
 * 取餐方式：0 到店自取，1 外卖配送。
 */
public class OrderBean implements Serializable {

    public static final int STATUS_UNPAID = 0;
    public static final int STATUS_MAKING = 1;
    public static final int STATUS_DONE = 2;
    public static final int STATUS_CANCELED = 3;

    public static final int MODE_PICKUP = 0;
    public static final int MODE_DELIVERY = 1;

    /** 数据库自增 id */
    private long id;
    /** 展示用的订单号 */
    private String orderNo;
    /** 下单时间（毫秒时间戳） */
    private long createdAt;
    private final List<OrderItemBean> items = new ArrayList<>();
    /** 商品总价（优惠前） */
    private BigDecimal goodsTotal;
    /** 打包费 */
    private BigDecimal packingFee;
    /** 配送费（自取为 0） */
    private BigDecimal deliveryFee;
    /** 满减优惠金额 */
    private BigDecimal discount;
    /** 实付金额 = goodsTotal + packingFee + deliveryFee - discount */
    private BigDecimal total;
    private int status;
    private int mode;
    /** 取餐码（自取）/ 收货地址（外送） */
    private String pickupCode;
    private String address;
    private String remark;

    public void addItem(OrderItemBean item) {
        items.add(item);
    }

    /** 商品总件数 */
    public int getItemCount() {
        int count = 0;
        for (OrderItemBean item : items) {
            count += item.getCount();
        }
        return count;
    }

    public String getStatusText() {
        switch (status) {
            case STATUS_UNPAID:
                return "待支付";
            case STATUS_MAKING:
                return "制作中";
            case STATUS_DONE:
                return "已完成";
            case STATUS_CANCELED:
                return "已取消";
            default:
                return "";
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderItemBean> getItems() {
        return items;
    }

    public BigDecimal getGoodsTotal() {
        return goodsTotal;
    }

    public void setGoodsTotal(BigDecimal goodsTotal) {
        this.goodsTotal = goodsTotal;
    }

    public BigDecimal getPackingFee() {
        return packingFee;
    }

    public void setPackingFee(BigDecimal packingFee) {
        this.packingFee = packingFee;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getPickupCode() {
        return pickupCode;
    }

    public void setPickupCode(String pickupCode) {
        this.pickupCode = pickupCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
