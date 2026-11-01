package cn.itcast.order.bean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 店铺信息实体类（对应教材 12.4.5 封装店铺信息）
 * 一个店铺包含基本信息 + 该店铺的菜品列表
 */
public class ShopBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;                        // 店铺Id
    private String shopName;               // 店铺名称
    private float score;                   // 店铺评分
    private int saleNum;                   // 月售数量
    private BigDecimal offerPrice;         // 起送价格
    private BigDecimal distributionCost;   // 配送费
    private String feature;                // 店铺特色（满减活动）
    private String time;                   // 配送时间
    private String category;               // 店铺分类
    private String banner;                 // 店铺图片名称
    private List<FoodBean> foodList = new ArrayList<>();   // 菜品列表

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public int getSaleNum() {
        return saleNum;
    }

    public void setSaleNum(int saleNum) {
        this.saleNum = saleNum;
    }

    public BigDecimal getOfferPrice() {
        return offerPrice;
    }

    public void setOfferPrice(BigDecimal offerPrice) {
        this.offerPrice = offerPrice;
    }

    public BigDecimal getDistributionCost() {
        return distributionCost;
    }

    public void setDistributionCost(BigDecimal distributionCost) {
        this.distributionCost = distributionCost;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
    }

    public List<FoodBean> getFoodList() {
        return foodList;
    }

    public void setFoodList(List<FoodBean> foodList) {
        this.foodList = foodList;
    }

    /** 起送价文本：¥20起送 */
    public String getOfferText() {
        return "¥" + money(offerPrice) + "起送";
    }

    /** 配送费文本：配送费¥5 */
    public String getDistributionText() {
        return "配送费¥" + money(distributionCost);
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }
}
