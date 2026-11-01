package cn.itcast.order.utils;

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

import cn.itcast.order.bean.FoodBean;
import cn.itcast.order.bean.ShopBean;

/**
 * 数据工具类：负责“获取服务器 JSON 数据 + 解析 JSON”
 * 对应教材 12.3 服务器数据准备、12.4.8 实现店铺列表界面显示功能
 *
 * 本项目默认读取 assets/shop.json（内容与放在 Tomcat 上完全一样），
 * 把 Constant.USE_SERVER 改为 true 即可切换成真正的网络请求。
 */
public class ShopDataUtils {

    /** 获取店铺 JSON 数据字符串 */
    public static String loadShopJson(Context context) {
        if (Constant.USE_SERVER) {
            String json = HttpUtil.get(Constant.SHOP_JSON_URL);
            if (json != null && !json.trim().isEmpty()) {
                return json;
            }
        }
        return readAssets(context, "shop.json");
    }

    /** 解析店铺列表 */
    public static List<ShopBean> parseShopList(String json) {
        List<ShopBean> shops = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(json);
            JSONArray array = root.optJSONArray("shops");
            if (array == null) {
                return shops;
            }
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                ShopBean shop = new ShopBean();
                shop.setId(obj.optInt("id"));
                shop.setShopName(obj.optString("shopName"));
                shop.setScore((float) obj.optDouble("score", 5.0));
                shop.setSaleNum(obj.optInt("saleNum"));
                shop.setOfferPrice(new BigDecimal(obj.optString("offerPrice", "0")));
                shop.setDistributionCost(new BigDecimal(obj.optString("distributionCost", "0")));
                shop.setFeature(obj.optString("feature"));
                shop.setTime(obj.optString("time"));
                shop.setCategory(obj.optString("category"));
                shop.setBanner(obj.optString("banner"));
                shop.setFoodList(parseFoodList(obj.optJSONArray("foodList")));
                shops.add(shop);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return shops;
    }

    /** 解析单个店铺的菜品列表 */
    private static List<FoodBean> parseFoodList(JSONArray array) {
        List<FoodBean> foods = new ArrayList<>();
        if (array == null) {
            return foods;
        }
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject obj = array.getJSONObject(i);
                FoodBean food = new FoodBean();
                food.setFoodId(obj.optInt("foodId"));
                food.setFoodName(obj.optString("foodName"));
                food.setPopularity(obj.optString("popularity"));
                food.setSaleNum(obj.optInt("saleNum"));
                food.setPrice(new BigDecimal(obj.optString("price", "0")));
                food.setFoodPic(obj.optString("foodPic"));
                foods.add(food);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return foods;
    }

    /** 解析广告栏图片名称列表 */
    public static List<String> parseBanners(String json) {
        List<String> banners = new ArrayList<>();
        try {
            JSONArray array = new JSONObject(json).optJSONArray("banners");
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    banners.add(array.optString(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return banners;
    }

    /** 根据图片名称取得 drawable 资源 id，等价于教材中从服务器下载图片后的占位逻辑 */
    public static int getDrawableId(Context context, String name) {
        if (name == null || name.trim().isEmpty()) {
            return 0;
        }
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
    }

    /** 读取 assets 目录下的文本文件 */
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
