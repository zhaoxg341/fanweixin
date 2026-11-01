package cn.itcast.milktea.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品（奶茶 / 小食）。
 * 实现 Serializable 是为了可以在 Activity 之间用 Intent 直接传递整个对象。
 */
public class GoodsBean implements Serializable {

    private int goodsId;
    private int categoryId;
    private String name;
    private String desc;
    /** 原价（现价） */
    private BigDecimal price;
    /** 划线价，0 表示没有优惠 */
    private BigDecimal originalPrice;
    private String pic;
    private int sales;
    /** 角标文案：人气TOP1 / 新品 / 店长推荐，空字符串表示不显示 */
    private String badge;

    public BigDecimal getPrice() {
        return price == null ? BigDecimal.ZERO : price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        if (originalPrice == null) {
            return BigDecimal.ZERO;
        }
        return originalPrice.compareTo(getPrice()) > 0 ? originalPrice : BigDecimal.ZERO;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    /** 价格文本：¥12.00 */
    public String getPriceString() {
        return "¥" + getPrice().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
    }

    public boolean hasBadge() {
        return badge != null && !badge.trim().isEmpty();
    }

    public int getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(int goodsId) {
        this.goodsId = goodsId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public int getSales() {
        return sales;
    }

    public void setSales(int sales) {
        this.sales = sales;
    }

    public String getBadge() {
        return badge == null ? "" : badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }
}
