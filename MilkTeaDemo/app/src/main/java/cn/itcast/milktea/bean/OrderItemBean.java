package cn.itcast.milktea.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单里的一行商品（名称 + 规格 + 单价 + 数量）。
 * 从购物车结算时把每个 CartItem 转成它，随订单一起写进数据库。
 */
public class OrderItemBean implements Serializable {

    /** 对应 GoodsBean.goodsId，"再来一单"时靠它找回商品 */
    private int goodsId;
    private String name;
    private String specText;
    private BigDecimal unitPrice;
    private int count;

    public OrderItemBean() {
    }

    public OrderItemBean(int goodsId, String name, String specText,
                         BigDecimal unitPrice, int count) {
        this.goodsId = goodsId;
        this.name = name;
        this.specText = specText;
        this.unitPrice = unitPrice;
        this.count = count;
    }

    public int getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(int goodsId) {
        this.goodsId = goodsId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecText() {
        return specText;
    }

    public void setSpecText(String specText) {
        this.specText = specText;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
