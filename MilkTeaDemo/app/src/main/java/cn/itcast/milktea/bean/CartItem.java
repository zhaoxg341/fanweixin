package cn.itcast.milktea.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购物车条目。
 * 关键设计：同一个商品选不同规格（中杯冰 / 大杯热）是两行，
 * 所以用 key = goodsId + 规格文本 作为唯一标识，而不是只用 goodsId。
 */
public class CartItem implements Serializable {

    /** 唯一标识：goodsId + 规格文本 */
    private final String key;
    private final int goodsId;
    private final String name;
    private final String pic;
    /** 单价 = 商品原价 + 杯型加价 + 加料加价 */
    private final BigDecimal unitPrice;
    /** 规格文本，如 "大杯/热/半糖/加珍珠、芝士奶盖" */
    private final String specText;
    private int count;

    public CartItem(String key, int goodsId, String name, String pic,
                    BigDecimal unitPrice, String specText, int count) {
        this.key = key;
        this.goodsId = goodsId;
        this.name = name;
        this.pic = pic;
        this.unitPrice = unitPrice;
        this.specText = specText;
        this.count = count;
    }

    /** 小计 = 单价 × 数量 */
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(new BigDecimal(count));
    }

    public String getKey() {
        return key;
    }

    public int getGoodsId() {
        return goodsId;
    }

    public String getName() {
        return name;
    }

    public String getPic() {
        return pic;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getSpecText() {
        return specText;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
