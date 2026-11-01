package cn.itcast.milktea.utils;

import java.math.BigDecimal;

/**
 * 全局常量。
 */
public final class Constant {

    private Constant() {
    }

    /** assets 里的数据文件名 */
    public static final String DATA_FILE = "milktea.json";

    /** 打包费：每件 1 元，最多收 3 元 */
    public static final BigDecimal PACKING_FEE_PER_ITEM = new BigDecimal("1");
    public static final int PACKING_FEE_MAX_ITEMS = 3;

    /** 加料统一加价展示文案 */
    public static final String TOPPING_SUFFIX = "加";
}
