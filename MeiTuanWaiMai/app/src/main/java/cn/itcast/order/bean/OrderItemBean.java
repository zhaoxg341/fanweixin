package cn.itcast.order.bean;

import java.math.BigDecimal;

/**
 * 订单中的单个商品快照。
 * 下单时把菜品信息复制一份存进订单，之后菜品改价也不影响历史订单。
 */
public class OrderItemBean {

    private int foodId;
    private String foodName;
    private String foodPic;
    private BigDecimal price;
    private int count;

    public OrderItemBean() {
    }

    public OrderItemBean(int foodId, String foodName, String foodPic, BigDecimal price, int count) {
        this.foodId = foodId;
        this.foodName = foodName;
        this.foodPic = foodPic;
        this.price = price;
        this.count = count;
    }

    public int getFoodId() {
        return foodId;
    }

    public void setFoodId(int foodId) {
        this.foodId = foodId;
    }

    public String getFoodName() {
        return foodName == null ? "" : foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getFoodPic() {
        return foodPic == null ? "" : foodPic;
    }

    public void setFoodPic(String foodPic) {
        this.foodPic = foodPic;
    }

    public BigDecimal getPrice() {
        return price == null ? BigDecimal.ZERO : price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public BigDecimal getSubtotal() {
        return getPrice().multiply(new BigDecimal(count));
    }
}
