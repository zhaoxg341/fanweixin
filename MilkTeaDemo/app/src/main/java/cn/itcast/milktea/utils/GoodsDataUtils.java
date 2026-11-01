package cn.itcast.milktea.utils;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import cn.itcast.milktea.bean.CategoryBean;
import cn.itcast.milktea.bean.GoodsBean;
import cn.itcast.milktea.bean.OptionBean;
import cn.itcast.milktea.bean.ShopBean;

/**
 * 数据工具类：负责"读取 assets/milktea.json + 解析成 Bean 对象"。
 *
 * 数据在第一次使用时解析一次，之后一直用内存里的静态缓存，
 * 因为 ListView 每滑动一下都可能要取数据，反复读文件太浪费。
 */
public class GoodsDataUtils {

    private static boolean inited = false;

    private static ShopBean shop;
    private static final List<CategoryBean> CATEGORIES = new ArrayList<>();
    private static final List<GoodsBean> GOODS = new ArrayList<>();
    /** 杯型（带加价） */
    private static final List<OptionBean> SIZES = new ArrayList<>();
    /** 温度（不加价） */
    private static final List<String> TEMPS = new ArrayList<>();
    /** 糖度（不加价） */
    private static final List<String> SUGARS = new ArrayList<>();
    /** 加料（带加价） */
    private static final List<OptionBean> TOPPINGS = new ArrayList<>();
    /** 满减规则：0 = 满 25 减 4，1 = 满 40 减 10 */
    private static final BigDecimal[] COUPON_THRESHOLD = new BigDecimal[2];
    private static final BigDecimal[] COUPON_REDUCE = new BigDecimal[2];

    /** 确保数据已解析（所有取数据的入口都先调它） */
    private static void ensureInited(Context context) {
        if (inited) {
            return;
        }
        parse(readAssets(context, Constant.DATA_FILE));
        inited = true;
    }

    public static ShopBean getShop(Context context) {
        ensureInited(context);
        return shop;
    }

    public static List<CategoryBean> getCategories(Context context) {
        ensureInited(context);
        return CATEGORIES;
    }

    public static List<GoodsBean> getGoods(Context context) {
        ensureInited(context);
        return GOODS;
    }

    public static List<OptionBean> getSizes(Context context) {
        ensureInited(context);
        return SIZES;
    }

    public static List<String> getTemps(Context context) {
        ensureInited(context);
        return TEMPS;
    }

    public static List<String> getSugars(Context context) {
        ensureInited(context);
        return SUGARS;
    }

    public static List<OptionBean> getToppings(Context context) {
        ensureInited(context);
        return TOPPINGS;
    }

    /** 按分类筛选商品 */
    public static List<GoodsBean> getGoodsByCategory(Context context, int categoryId) {
        ensureInited(context);
        List<GoodsBean> list = new ArrayList<>();
        for (GoodsBean goods : GOODS) {
            if (goods.getCategoryId() == categoryId) {
                list.add(goods);
            }
        }
        return list;
    }

    /** 按 goodsId 找商品（"再来一单"时要用） */
    public static GoodsBean findGoods(Context context, int goodsId) {
        ensureInited(context);
        for (GoodsBean goods : GOODS) {
            if (goods.getGoodsId() == goodsId) {
                return goods;
            }
        }
        return null;
    }

    /** 按关键词搜索商品（名称或描述包含即可） */
    public static List<GoodsBean> search(Context context, String keyword) {
        ensureInited(context);
        List<GoodsBean> list = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return list;
        }
        String key = keyword.trim().toLowerCase();
        for (GoodsBean goods : GOODS) {
            if (goods.getName().toLowerCase().contains(key)
                    || goods.getDesc().toLowerCase().contains(key)) {
                list.add(goods);
            }
        }
        return list;
    }

    /**
     * 满减金额：取"满足门槛且优惠最大"的那一档。
     * 例如 42 元同时满足"满25减4"和"满40减10"，返回 10。
     */
    public static BigDecimal couponDiscount(Context context, BigDecimal goodsTotal) {
        ensureInited(context);
        BigDecimal best = BigDecimal.ZERO;
        for (int i = 0; i < COUPON_THRESHOLD.length; i++) {
            if (COUPON_THRESHOLD[i] != null && goodsTotal.compareTo(COUPON_THRESHOLD[i]) >= 0
                    && COUPON_REDUCE[i].compareTo(best) > 0) {
                best = COUPON_REDUCE[i];
            }
        }
        return best;
    }

    /**
     * 还差多少钱能享受下一档满减，用于首页购物车栏提示。
     * 例："再买 ¥13.00 可减 ¥4"
     */
    public static String nextCouponTip(Context context, BigDecimal goodsTotal) {
        ensureInited(context);
        BigDecimal nearest = null;
        BigDecimal reduce = null;
        for (int i = 0; i < COUPON_THRESHOLD.length; i++) {
            if (COUPON_THRESHOLD[i] == null) {
                continue;
            }
            if (goodsTotal.compareTo(COUPON_THRESHOLD[i]) < 0
                    && (nearest == null || COUPON_THRESHOLD[i].compareTo(nearest) < 0)) {
                nearest = COUPON_THRESHOLD[i];
                reduce = COUPON_REDUCE[i];
            }
        }
        if (nearest == null) {
            return "已享受最大满减优惠";
        }
        BigDecimal diff = nearest.subtract(goodsTotal);
        return "再买 ¥" + diff.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()
                + " 可减 ¥" + reduce.intValue();
    }

    /** 满减提示文案，如 "满25减4，满40减10" */
    public static String couponText(Context context) {        ensureInited(context);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < COUPON_THRESHOLD.length; i++) {
            if (COUPON_THRESHOLD[i] == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("，");
            }
            sb.append("满").append(COUPON_THRESHOLD[i].intValue())
                    .append("减").append(COUPON_REDUCE[i].intValue());
        }
        return sb.toString();
    }

    /** 根据图片名取 drawable 资源 id，取不到返回 0 */
    public static int getDrawableId(Context context, String name) {
        if (name == null || name.trim().isEmpty()) {
            return 0;
        }
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
    }

    // ---------------- 内部解析 ----------------

    private static void parse(String json) {
        try {
            JSONObject root = new JSONObject(json);

            JSONObject shopObj = root.optJSONObject("shop");
            if (shopObj != null) {
                shop = new ShopBean();
                shop.setName(shopObj.optString("name"));
                shop.setScore((float) shopObj.optDouble("score", 5.0));
                shop.setMonthlySale(shopObj.optInt("monthlySale"));
                shop.setDeliveryTime(shopObj.optString("deliveryTime"));
                shop.setDeliveryFee(new BigDecimal(shopObj.optString("deliveryFee", "0")));
                shop.setNotice(shopObj.optString("notice"));
                shop.setBanner(shopObj.optString("banner"));
                shop.setAddress(shopObj.optString("address"));
                shop.setOpenTime(shopObj.optString("openTime"));
            }

            JSONArray cats = root.optJSONArray("categories");
            for (int i = 0; cats != null && i < cats.length(); i++) {
                JSONObject obj = cats.getJSONObject(i);
                CATEGORIES.add(new CategoryBean(obj.optInt("id"), obj.optString("name")));
            }

            JSONArray goodsArr = root.optJSONArray("goods");
            for (int i = 0; goodsArr != null && i < goodsArr.length(); i++) {
                JSONObject obj = goodsArr.getJSONObject(i);
                GoodsBean goods = new GoodsBean();
                goods.setGoodsId(obj.optInt("goodsId"));
                goods.setCategoryId(obj.optInt("categoryId"));
                goods.setName(obj.optString("name"));
                goods.setDesc(obj.optString("desc"));
                goods.setPrice(new BigDecimal(obj.optString("price", "0")));
                goods.setOriginalPrice(new BigDecimal(obj.optString("originalPrice", "0")));
                goods.setPic(obj.optString("pic"));
                goods.setSales(obj.optInt("sales"));
                goods.setBadge(obj.optString("badge"));
                GOODS.add(goods);
            }

            JSONObject specs = root.optJSONObject("specs");
            if (specs != null) {
                JSONArray sizes = specs.optJSONArray("sizes");
                for (int i = 0; sizes != null && i < sizes.length(); i++) {
                    JSONObject obj = sizes.getJSONObject(i);
                    SIZES.add(new OptionBean(obj.optString("name"),
                            new BigDecimal(obj.optString("extra", "0"))));
                }
                JSONArray temps = specs.optJSONArray("temps");
                for (int i = 0; temps != null && i < temps.length(); i++) {
                    TEMPS.add(temps.optString(i));
                }
                JSONArray sugars = specs.optJSONArray("sugars");
                for (int i = 0; sugars != null && i < sugars.length(); i++) {
                    SUGARS.add(sugars.optString(i));
                }
                JSONArray toppings = specs.optJSONArray("toppings");
                for (int i = 0; toppings != null && i < toppings.length(); i++) {
                    JSONObject obj = toppings.getJSONObject(i);
                    TOPPINGS.add(new OptionBean(obj.optString("name"),
                            new BigDecimal(obj.optString("extra", "0"))));
                }
            }

            JSONArray coupons = root.optJSONArray("coupons");
            for (int i = 0; coupons != null && i < coupons.length() && i < 2; i++) {
                JSONObject obj = coupons.getJSONObject(i);
                COUPON_THRESHOLD[i] = new BigDecimal(obj.optString("threshold", "0"));
                COUPON_REDUCE[i] = new BigDecimal(obj.optString("reduce", "0"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 读 assets 文本文件 */
    private static String readAssets(Context context, String fileName) {
        InputStream is = null;
        try {
            is = context.getAssets().open(fileName);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }
}
