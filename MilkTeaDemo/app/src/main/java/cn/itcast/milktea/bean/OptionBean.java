package cn.itcast.milktea.bean;

import java.math.BigDecimal;

/**
 * 规格选项：可以是"杯型"（中杯 +0 / 大杯 +3），也可以是"加料"（珍珠 +2 …）。
 * 温度和糖度没有加价，extra 为 0。
 */
public class OptionBean {

    private final String name;
    /** 该选项需要额外加的钱 */
    private final BigDecimal extra;

    public OptionBean(String name, BigDecimal extra) {
        this.name = name;
        this.extra = extra == null ? BigDecimal.ZERO : extra;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getExtra() {
        return extra;
    }
}
