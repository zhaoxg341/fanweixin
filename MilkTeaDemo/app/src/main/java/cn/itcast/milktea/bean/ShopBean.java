package cn.itcast.milktea.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 门店信息（本 Demo 只有一家店）。
 */
public class ShopBean implements Serializable {

    private String name;
    private float score;
    private int monthlySale;
    /** 预计出餐时间，如 "25分钟" */
    private String deliveryTime;
    private BigDecimal deliveryFee;
    private String notice;
    private String banner;
    private String address;
    private String openTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public int getMonthlySale() {
        return monthlySale;
    }

    public void setMonthlySale(int monthlySale) {
        this.monthlySale = monthlySale;
    }

    public String getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(String deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee == null ? BigDecimal.ZERO : deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getOpenTime() {
        return openTime;
    }

    public void setOpenTime(String openTime) {
        this.openTime = openTime;
    }
}
