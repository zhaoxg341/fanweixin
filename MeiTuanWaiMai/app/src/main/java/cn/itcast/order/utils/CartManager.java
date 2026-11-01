package cn.itcast.order.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.itcast.order.bean.FoodBean;

/**
 * 购物车管理类（单例思想的静态实现）
 * 用两个 Map 分别保存“菜品对象”和“数量”，避免菜品对象在 Activity 间
 * 通过 Intent 传递（会被序列化成副本）后数量对不上的问题。
 */
public class CartManager {

    /** 当前购物车所属的店铺 Id，切换店铺时自动清空 */
    private static int currentShopId = -1;

    private static final Map<Integer, FoodBean> FOODS = new LinkedHashMap<>();
    private static final Map<Integer, Integer> COUNTS = new LinkedHashMap<>();

    private CartManager() {
    }

    /** 进入店铺时调用：换了店铺就清空上一次点的菜 */
    public static void switchShop(int shopId) {
        if (currentShopId != shopId) {
            clearInternal();
            currentShopId = shopId;
        }
    }

    public static int getCurrentShopId() {
        return currentShopId;
    }

    /** 把某道菜的数量 +1 */
    public static void add(FoodBean food) {
        if (food == null) {
            return;
        }
        FOODS.put(food.getFoodId(), food);
        COUNTS.put(food.getFoodId(), getCount(food.getFoodId()) + 1);
    }

    /** 把某道菜的数量 -1，减到 0 就从购物车移除 */
    public static void reduce(int foodId) {
        int count = getCount(foodId) - 1;
        if (count <= 0) {
            COUNTS.remove(foodId);
            FOODS.remove(foodId);
        } else {
            COUNTS.put(foodId, count);
        }
    }

    /** 查询某道菜在购物车中的数量 */
    public static int getCount(int foodId) {
        Integer count = COUNTS.get(foodId);
        return count == null ? 0 : count;
    }

    /** 购物车里的所有菜品（带数量） */
    public static List<FoodBean> getCartList() {
        List<FoodBean> list = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : COUNTS.entrySet()) {
            FoodBean food = FOODS.get(entry.getKey());
            if (food != null) {
                food.setCount(entry.getValue());
                list.add(food);
            }
        }
        return list;
    }

    /** 购物车中商品总件数 */
    public static int getTotalCount() {
        int total = 0;
        for (Integer count : COUNTS.values()) {
            total += count;
        }
        return total;
    }

    /** 购物车中商品总金额 */
    public static BigDecimal getTotalPrice() {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, Integer> entry : COUNTS.entrySet()) {
            FoodBean food = FOODS.get(entry.getKey());
            if (food != null && food.getPrice() != null) {
                total = total.add(food.getPrice().multiply(new BigDecimal(entry.getValue())));
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean isEmpty() {
        return COUNTS.isEmpty();
    }

    public static void clear() {
        clearInternal();
    }

    private static void clearInternal() {
        FOODS.clear();
        COUNTS.clear();
    }
}
