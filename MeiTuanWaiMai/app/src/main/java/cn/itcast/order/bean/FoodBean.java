package cn.itcast.order.bean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 菜品信息实体类（对应教材 12.4.5 封装菜品信息）
 * 实现 Serializable 是因为菜品对象需要通过 Intent 在 Activity 之间传递
 */
public class FoodBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private int foodId;          // 菜品Id
    private String foodName;     // 菜品名称
    private String popularity;   // 人气标签，如“人气爆款”
    private int saleNum;         // 月售量
    private BigDecimal price;    // 菜品单价
    private String foodPic;      // 菜品图片名称（对应 res/drawable-nodpi 下的图片）
    private int count;           // 加入购物车的数量

    public int getFoodId() {
        return foodId;
    }

    public void setFoodId(int foodId) {
        this.foodId = foodId;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getPopularity() {
        return popularity;
    }

    public void setPopularity(String popularity) {
        this.popularity = popularity;
    }

    public int getSaleNum() {
        return saleNum;
    }

    public void setSaleNum(int saleNum) {
        this.saleNum = saleNum;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getFoodPic() {
        return foodPic;
    }

    public void setFoodPic(String foodPic) {
        this.foodPic = foodPic;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    /** 价格文本：¥18.50 */
    public String getPriceString() {
        if (price == null) {
            return "¥0.00";
        }
        return "¥" + price.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** 小计金额 = 单价 × 数量 */
    public BigDecimal getSubtotal() {
        BigDecimal base = price == null ? BigDecimal.ZERO : price;
        return base.multiply(new BigDecimal(count)).setScale(2, RoundingMode.HALF_UP);
    }
}
