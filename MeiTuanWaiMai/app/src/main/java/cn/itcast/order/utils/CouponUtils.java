package cn.itcast.order.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.itcast.order.bean.CouponBean;
import cn.itcast.order.bean.ShopBean;

/**
 * 优惠计算工具。
 * 店铺活动写在 shop.json 的 feature 字段里（例如“满30减10”），
 * 这里用正则解析出来，转成一张可用的店铺券；
 * 另外再给一张全场平台券，模拟美团的“神券”。
 */
public class CouponUtils {

    /** 匹配“满30减10”“满50减15”这类活动文案 */
    private static final Pattern PATTERN = Pattern.compile("满(\\d+(?:\\.\\d+)?)减(\\d+(?:\\.\\d+)?)");

    private CouponUtils() {
    }

    /** 解析店铺满减，解析不到返回 null（比如“第二杯半价”） */
    public static CouponBean parseShopCoupon(ShopBean shop) {
        if (shop == null || shop.getFeature() == null) {
            return null;
        }
        Matcher matcher = PATTERN.matcher(shop.getFeature());
        if (!matcher.find()) {
            return null;
        }
        try {
            BigDecimal threshold = new BigDecimal(matcher.group(1));
            BigDecimal amount = new BigDecimal(matcher.group(2));
            return new CouponBean("shop_" + shop.getId(), shop.getFeature(), "店铺券",
                    threshold, amount);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 平台神券：满50减8（全场通用） */
    public static CouponBean platformCoupon() {
        return new CouponBean("platform_1", "满50减8", "平台券",
                new BigDecimal("50"), new BigDecimal("8"));
    }

    /**
     * 当前订单可选的优惠券列表。
     * usable 字段会按商品金额计算，界面据此显示灰色不可用状态。
     */
    public static List<CouponBean> buildCoupons(ShopBean shop, BigDecimal goodsPrice) {
        List<CouponBean> list = new ArrayList<>();
        CouponBean shopCoupon = parseShopCoupon(shop);
        if (shopCoupon != null) {
            shopCoupon.setUsable(goodsPrice.compareTo(shopCoupon.getThreshold()) >= 0);
            list.add(shopCoupon);
        }
        CouponBean platform = platformCoupon();
        platform.setUsable(goodsPrice.compareTo(platform.getThreshold()) >= 0);
        list.add(platform);
        return list;
    }

    /**
     * 自动挑最优惠的一张券（只考虑可用券）。
     * 没有可用券时返回 null。
     */
    public static CouponBean bestCoupon(ShopBean shop, BigDecimal goodsPrice) {
        CouponBean best = null;
        for (CouponBean coupon : buildCoupons(shop, goodsPrice)) {
            if (!coupon.isUsable()) {
                continue;
            }
            if (best == null || coupon.getAmount().compareTo(best.getAmount()) > 0) {
                best = coupon;
            }
        }
        return best;
    }

    /** 优惠金额不会超过商品金额，避免出现负数实付 */
    public static BigDecimal safeDiscount(BigDecimal discount, BigDecimal goodsPrice) {
        if (discount == null) {
            return BigDecimal.ZERO;
        }
        if (discount.compareTo(goodsPrice) > 0) {
            return goodsPrice;
        }
        return discount;
    }
}
