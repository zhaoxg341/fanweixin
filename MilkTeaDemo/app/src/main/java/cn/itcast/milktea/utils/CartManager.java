package cn.itcast.milktea.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.itcast.milktea.bean.CartItem;
import cn.itcast.milktea.bean.GoodsBean;
import cn.itcast.milktea.bean.OrderBean;
import cn.itcast.milktea.bean.OrderItemBean;

/**
 * 购物车管理（静态单例）。
 *
 * 为什么用 Map 而不是 List：
 *   同一个奶茶选"中杯/冰/半糖"和"大杯/热/无糖"应该是两行，所以 key 不能只用 goodsId，
 *   而是 goodsId + 规格文本；Map 天然可以去重、按下标增删也方便。
 *
 * 为什么用 LinkedHashMap：
 *   它保持插入顺序，购物车列表不会因为 HashMap 的乱序而每次刷新顺序都变。
 */
public class CartManager {

    private static final Map<String, CartItem> ITEMS = new LinkedHashMap<>();

    private CartManager() {
    }

    /** 购物车条目的唯一键：商品 id + 规格 */
    public static String buildKey(int goodsId, String specText) {
        return goodsId + "|" + (specText == null ? "" : specText);
    }

    /**
     * 加入购物车；同商品同规格会累加数量。
     *
     * @param unitPrice 已经算好杯型和加料加价的单价
     */
    public static void add(GoodsBean goods, String specText, BigDecimal unitPrice, int count) {
        if (goods == null || count <= 0) {
            return;
        }
        String key = buildKey(goods.getGoodsId(), specText);
        CartItem item = ITEMS.get(key);
        if (item == null) {
            ITEMS.put(key, new CartItem(key, goods.getGoodsId(), goods.getName(),
                    goods.getPic(), unitPrice, specText, count));
        } else {
            item.setCount(item.getCount() + count);
        }
    }

    /** 数量 +1 */
    public static void increase(String key) {
        CartItem item = ITEMS.get(key);
        if (item != null) {
            item.setCount(item.getCount() + 1);
        }
    }

    /** 数量 -1，减到 0 自动移除 */
    public static void decrease(String key) {
        CartItem item = ITEMS.get(key);
        if (item == null) {
            return;
        }
        if (item.getCount() <= 1) {
            ITEMS.remove(key);
        } else {
            item.setCount(item.getCount() - 1);
        }
    }

    /** 某个商品在所有规格下的总数量（首页列表的加减号用） */
    public static int getCountByGoods(int goodsId) {
        int total = 0;
        for (CartItem item : ITEMS.values()) {
            if (item.getGoodsId() == goodsId) {
                total += item.getCount();
            }
        }
        return total;
    }

    /** 首页"+"：按默认规格加一杯（中杯/冰/七分糖） */
    public static void addDefault(GoodsBean goods) {
        add(goods, "中杯/冰/七分糖", goods.getPrice(), 1);
    }

    /** 首页"−"：把该商品数量减 1（优先减最后加入的那条规格） */
    public static void decreaseByGoods(int goodsId) {
        String lastKey = null;
        for (CartItem item : ITEMS.values()) {
            if (item.getGoodsId() == goodsId) {
                lastKey = item.getKey();
            }
        }
        if (lastKey != null) {
            decrease(lastKey);
        }
    }

    public static List<CartItem> getCartList() {
        return new ArrayList<>(ITEMS.values());
    }

    public static int getTotalCount() {
        int total = 0;
        for (CartItem item : ITEMS.values()) {
            total += item.getCount();
        }
        return total;
    }

    /** 商品总价（不含打包费、配送费、优惠） */
    public static BigDecimal getGoodsTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : ITEMS.values()) {
            total = total.add(item.getSubtotal());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean isEmpty() {
        return ITEMS.isEmpty();
    }

    public static void clear() {
        ITEMS.clear();
    }

    /**
     * 再来一单：把历史订单里的商品按原规格、原单价重新放进购物车。
     * 找不到对应商品（数据改过）就跳过。
     */
    public static void reorder(android.content.Context context, OrderBean order) {
        for (OrderItemBean item : order.getItems()) {
            GoodsBean goods = GoodsDataUtils.findGoods(context, item.getGoodsId());
            if (goods != null) {
                add(goods, item.getSpecText(), item.getUnitPrice(), item.getCount());
            }
        }
    }
}
