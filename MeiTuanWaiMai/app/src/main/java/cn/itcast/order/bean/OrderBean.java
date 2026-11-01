package cn.itcast.order.bean;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单实体（对应数据库 t_order 表）
 * status：0 待支付 / 1 配送中 / 2 已完成 / 3 已取消
 */
public class OrderBean {

    public static final int STATUS_UNPAID = 0;
    public static final int STATUS_DELIVERING = 1;
    public static final int STATUS_DONE = 2;
    public static final int STATUS_CANCEL = 3;

    private long id;
    private String orderNo = "";
    private int shopId;
    private String shopName = "";
    private String shopBanner = "";
    private BigDecimal goodsPrice = BigDecimal.ZERO;
    private BigDecimal distribution = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private String address = "";
    private String receiver = "";
    private String phone = "";
    private String remark = "";
    private int tableware = 1;
    private String arriveTime = "";
    private int status = STATUS_UNPAID;
    private long createTime = System.currentTimeMillis();
    private List<OrderItemBean> items = new ArrayList<>();

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

    public int getShopId() {
        return shopId;
    }

    public void setShopId(int shopId) {
        this.shopId = shopId;
    }

    public String getShopName() {
        return shopName == null ? "" : shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getShopBanner() {
        return shopBanner == null ? "" : shopBanner;
    }

    public void setShopBanner(String shopBanner) {
        this.shopBanner = shopBanner;
    }

    public BigDecimal getGoodsPrice() {
        return goodsPrice == null ? BigDecimal.ZERO : goodsPrice;
    }

    public void setGoodsPrice(BigDecimal goodsPrice) {
        this.goodsPrice = goodsPrice;
    }

    public BigDecimal getDistribution() {
        return distribution == null ? BigDecimal.ZERO : distribution;
    }

    public void setDistribution(BigDecimal distribution) {
        this.distribution = distribution;
    }

    public BigDecimal getDiscount() {
        return discount == null ? BigDecimal.ZERO : discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTotal() {
        return total == null ? BigDecimal.ZERO : total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getAddress() {
        return address == null ? "" : address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getReceiver() {
        return receiver == null ? "" : receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getPhone() {
        return phone == null ? "" : phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRemark() {
        return remark == null ? "" : remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public int getTableware() {
        return tableware;
    }

    public void setTableware(int tableware) {
        this.tableware = tableware;
    }

    public String getArriveTime() {
        return arriveTime == null ? "" : arriveTime;
    }

    public void setArriveTime(String arriveTime) {
        this.arriveTime = arriveTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public List<OrderItemBean> getItems() {
        return items;
    }

    public void setItems(List<OrderItemBean> items) {
        this.items = items == null ? new ArrayList<OrderItemBean>() : items;
    }

    // ---------------- 展示辅助 ----------------

    public String getStatusText() {
        switch (status) {
            case STATUS_UNPAID:
                return "待支付";
            case STATUS_DELIVERING:
                return "配送中";
            case STATUS_DONE:
                return "已完成";
            case STATUS_CANCEL:
                return "已取消";
            default:
                return "未知";
        }
    }

    public String getTotalText() {
        return "¥" + getTotal().setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** 商品摘要：香辣鸡腿堡 等 3 件 */
    public String getSummary() {
        int count = 0;
        String firstName = "";
        for (OrderItemBean item : items) {
            if (firstName.isEmpty()) {
                firstName = item.getFoodName();
            }
            count += item.getCount();
        }
        if (firstName.isEmpty()) {
            return "暂无商品";
        }
        return firstName + " 等 " + count + " 件";
    }

    /** 列表上展示的店铺首图 */
    public String getShopPicName() {
        return shopBanner;
    }

    public boolean canCancel() {
        return status == STATUS_UNPAID || status == STATUS_DELIVERING;
    }

    public boolean canPay() {
        return status == STATUS_UNPAID;
    }

    public boolean canRebuy() {
        return status == STATUS_DONE || status == STATUS_CANCEL;
    }
}
