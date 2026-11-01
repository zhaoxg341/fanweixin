package cn.itcast.order.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.itcast.order.bean.AddressBean;

/**
 * 本地偏好设置统一入口。
 * 保存：昵称、手机号、收藏店铺、搜索历史、收货地址。
 * 全部用 SharedPreferences，卸载 App 后数据消失，符合演示项目定位。
 */
public class AppPrefs {

    private static final String NAME = "meituan_prefs";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_FAVORITES = "favorite_ids";
    private static final String KEY_HISTORY = "search_history";
    private static final String KEY_ADDRESS = "address_list";
    private static final String KEY_DEFAULT_ADDRESS = "default_address_id";

    private AppPrefs() {
    }

    private static SharedPreferences sp(Context context) {
        return context.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    // ---------------- 用户信息 ----------------

    public static String getNickname(Context context) {
        return sp(context).getString(KEY_NICKNAME, "美团用户");
    }

    public static void setNickname(Context context, String nickname) {
        sp(context).edit().putString(KEY_NICKNAME, nickname).apply();
    }

    public static String getPhone(Context context) {
        return sp(context).getString(KEY_PHONE, "138****8888");
    }

    public static void setPhone(Context context, String phone) {
        sp(context).edit().putString(KEY_PHONE, phone).apply();
    }

    // ---------------- 收藏店铺 ----------------

    public static boolean isFavorite(Context context, int shopId) {
        return getFavorites(context).contains(shopId);
    }

    /** 收藏 / 取消收藏，返回操作后是否处于收藏状态 */
    public static boolean toggleFavorite(Context context, int shopId) {
        Set<Integer> ids = getFavorites(context);
        boolean favorite;
        if (ids.contains(shopId)) {
            ids.remove(shopId);
            favorite = false;
        } else {
            ids.add(shopId);
            favorite = true;
        }
        StringBuilder sb = new StringBuilder();
        for (Integer id : ids) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(id);
        }
        sp(context).edit().putString(KEY_FAVORITES, sb.toString()).apply();
        return favorite;
    }

    public static Set<Integer> getFavorites(Context context) {
        Set<Integer> ids = new LinkedHashSet<>();
        String value = sp(context).getString(KEY_FAVORITES, "");
        if (value == null || value.trim().isEmpty()) {
            return ids;
        }
        for (String item : value.split(",")) {
            try {
                ids.add(Integer.parseInt(item.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    // ---------------- 搜索历史 ----------------

    public static List<String> getSearchHistory(Context context) {
        List<String> list = new ArrayList<>();
        String value = sp(context).getString(KEY_HISTORY, "");
        if (value == null || value.isEmpty()) {
            return list;
        }
        for (String item : value.split("\\|")) {
            if (!item.trim().isEmpty()) {
                list.add(item);
            }
        }
        return list;
    }

    /** 新搜索词插到最前面，最多保留 10 条 */
    public static void addSearchHistory(Context context, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        List<String> list = getSearchHistory(context);
        list.remove(keyword);
        list.add(0, keyword);
        while (list.size() > 10) {
            list.remove(list.size() - 1);
        }
        StringBuilder sb = new StringBuilder();
        for (String item : list) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append(item);
        }
        sp(context).edit().putString(KEY_HISTORY, sb.toString()).apply();
    }

    public static void clearSearchHistory(Context context) {
        sp(context).edit().remove(KEY_HISTORY).apply();
    }

    // ---------------- 收货地址 ----------------

    public static List<AddressBean> getAddresses(Context context) {
        List<AddressBean> list = new ArrayList<>();
        String value = sp(context).getString(KEY_ADDRESS, "");
        if (value == null || value.isEmpty()) {
            return list;
        }
        try {
            JSONArray array = new JSONArray(value);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                AddressBean bean = new AddressBean();
                bean.setId(obj.optLong("id"));
                bean.setName(obj.optString("name"));
                bean.setPhone(obj.optString("phone"));
                bean.setDetail(obj.optString("detail"));
                bean.setDefault(obj.optBoolean("isDefault"));
                list.add(bean);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void saveAddresses(Context context, List<AddressBean> list) {
        JSONArray array = new JSONArray();
        try {
            for (AddressBean bean : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", bean.getId());
                obj.put("name", bean.getName());
                obj.put("phone", bean.getPhone());
                obj.put("detail", bean.getDetail());
                obj.put("isDefault", bean.isDefault());
                array.put(obj);
            }
            sp(context).edit().putString(KEY_ADDRESS, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 默认地址：没有地址时返回 null */
    public static AddressBean getDefaultAddress(Context context) {
        List<AddressBean> list = getAddresses(context);
        if (list.isEmpty()) {
            return null;
        }
        for (AddressBean bean : list) {
            if (bean.isDefault()) {
                return bean;
            }
        }
        return list.get(0);
    }

    public static void setDefaultAddress(Context context, long id) {
        List<AddressBean> list = getAddresses(context);
        for (AddressBean bean : list) {
            bean.setDefault(bean.getId() == id);
        }
        saveAddresses(context, list);
    }

    /** 首次进入时写入一条示例地址，避免结算页空白 */
    public static void ensureDefaultAddress(Context context) {
        if (!getAddresses(context).isEmpty()) {
            return;
        }
        List<AddressBean> list = new ArrayList<>();
        AddressBean bean = new AddressBean();
        bean.setId(System.currentTimeMillis());
        bean.setName("李同学");
        bean.setPhone("138 8888 8888");
        bean.setDetail("湖南科技大学 · 计算机科学与工程学院 3 栋 402");
        bean.setDefault(true);
        list.add(bean);
        saveAddresses(context, list);
    }
}
