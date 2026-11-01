package cn.itcast.order.utils;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.order.bean.FoodBean;
import cn.itcast.order.bean.ShopBean;

/**
 * 店铺数据仓库：把 assets/shop.json 解析一次后缓存在内存中，
 * 首页、搜索页、收藏页、分类页都从这里取数据，避免重复读文件。
 */
public class ShopRepository {

    private static List<ShopBean> cache;
    private static List<String> bannerCache;

    private ShopRepository() {
    }

    public static synchronized List<ShopBean> getAll(Context context) {
        if (cache == null) {
            String json = ShopDataUtils.loadShopJson(context.getApplicationContext());
            cache = ShopDataUtils.parseShopList(json);
            bannerCache = ShopDataUtils.parseBanners(json);
        }
        return cache;
    }

    public static synchronized List<String> getBanners(Context context) {
        if (bannerCache == null) {
            getAll(context);
        }
        return bannerCache == null ? new ArrayList<String>() : bannerCache;
    }

    public static ShopBean findById(Context context, int shopId) {
        for (ShopBean shop : getAll(context)) {
            if (shop.getId() == shopId) {
                return shop;
            }
        }
        return null;
    }

    /**
     * 关键词搜索：店铺名命中，或者店内菜品名命中都算。
     * 关键词为空时返回全部。
     */
    public static List<ShopBean> search(Context context, String keyword) {
        List<ShopBean> result = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            result.addAll(getAll(context));
            return result;
        }
        String key = keyword.trim().toLowerCase();
        for (ShopBean shop : getAll(context)) {
            if (matchShop(shop, key)) {
                result.add(shop);
            }
        }
        return result;
    }

    private static boolean matchShop(ShopBean shop, String key) {
        if (shop.getShopName() != null && shop.getShopName().toLowerCase().contains(key)) {
            return true;
        }
        if (shop.getCategory() != null && shop.getCategory().toLowerCase().contains(key)) {
            return true;
        }
        for (FoodBean food : shop.getFoodList()) {
            if (food.getFoodName() != null && food.getFoodName().toLowerCase().contains(key)) {
                return true;
            }
        }
        return false;
    }

    /** 分类频道：支持传入多个分类关键词，用 | 分隔 */
    public static List<ShopBean> byCategory(Context context, String categories) {
        if (categories == null || categories.trim().isEmpty() || "全部".equals(categories)) {
            return new ArrayList<>(getAll(context));
        }
        List<ShopBean> result = new ArrayList<>();
        String[] keys = categories.split("\\|");
        for (ShopBean shop : getAll(context)) {
            String category = shop.getCategory() == null ? "" : shop.getCategory();
            for (String key : keys) {
                if (category.contains(key)) {
                    result.add(shop);
                    break;
                }
            }
        }
        return result;
    }

    /** 收藏的店铺，按收藏顺序返回 */
    public static List<ShopBean> getFavorites(Context context) {
        List<ShopBean> result = new ArrayList<>();
        for (Integer id : AppPrefs.getFavorites(context)) {
            ShopBean shop = findById(context, id);
            if (shop != null) {
                result.add(shop);
            }
        }
        return result;
    }
}
