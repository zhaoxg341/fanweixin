package cn.itcast.milktea.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cn.itcast.milktea.bean.OrderBean;
import cn.itcast.milktea.bean.ShopBean;

/**
 * 金额计算 + 格式化工具。
 *
 * 金额统一用 BigDecimal：double 算钱会出现 0.1 + 0.2 = 0.30000000000000004 这种问题，
 * 课设答辩时这也是个可以讲的知识点。
 */
public class PriceUtils {

    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);

    private PriceUtils() {
    }

    /** ¥12.00 */
    public static String format(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        return "¥" + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** 12.00（不带货币符号） */
    public static String plain(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** 打包费：每件 1 元，最多按 3 件收 */
    public static BigDecimal packingFee(int totalCount) {
        int charged = Math.min(totalCount, Constant.PACKING_FEE_MAX_ITEMS);
        return Constant.PACKING_FEE_PER_ITEM.multiply(new BigDecimal(charged));
    }

    /** 配送费：只有外送才收 */
    public static BigDecimal deliveryFee(ShopBean shop, int mode) {
        if (mode != OrderBean.MODE_DELIVERY || shop == null) {
            return BigDecimal.ZERO;
        }
        return shop.getDeliveryFee();
    }

    /** 实付 = 商品 + 打包 + 配送 - 满减（最低 0） */
    public static BigDecimal total(BigDecimal goodsTotal, BigDecimal packing,
                                   BigDecimal delivery, BigDecimal discount) {
        BigDecimal total = safe(goodsTotal).add(safe(packing)).add(safe(delivery))
                .subtract(safe(discount));
        return total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total;
    }

    /** 时间戳 → 2026-10-19 15:30 */
    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
