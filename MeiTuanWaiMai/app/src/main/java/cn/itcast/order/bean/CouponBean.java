package cn.itcast.order.bean;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 优惠券：店铺满减券 / 平台券共用这个实体
 */
public class CouponBean {

    private String id;
    private String title;        // 满30减10
    private String scope;        // 店铺券 / 平台券
    private BigDecimal threshold = BigDecimal.ZERO;  // 使用门槛
    private BigDecimal amount = BigDecimal.ZERO;     // 减免金额
    private boolean usable = true;                   // 当前订单是否可用

    public CouponBean() {
    }

    public CouponBean(String id, String title, String scope,
                      BigDecimal threshold, BigDecimal amount) {
        this.id = id;
        this.title = title;
        this.scope = scope;
        this.threshold = threshold;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title == null ? "" : title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getScope() {
        return scope == null ? "" : scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public BigDecimal getThreshold() {
        return threshold == null ? BigDecimal.ZERO : threshold;
    }

    public void setThreshold(BigDecimal threshold) {
        this.threshold = threshold;
    }

    public BigDecimal getAmount() {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public boolean isUsable() {
        return usable;
    }

    public void setUsable(boolean usable) {
        this.usable = usable;
    }

    public String getThresholdText() {
        if (getThreshold().compareTo(BigDecimal.ZERO) <= 0) {
            return "无门槛";
        }
        return "满" + getThreshold().stripTrailingZeros().toPlainString() + "元可用";
    }

    public String getAmountText() {
        return "¥" + getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public String getUnusableText() {
        return "还差 ¥" + getThreshold().setScale(2, RoundingMode.HALF_UP).toPlainString() + " 可用";
    }
}
