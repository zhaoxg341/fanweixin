package cn.itcast.order.utils;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.order.bean.ReviewBean;
import cn.itcast.order.bean.ShopBean;

/**
 * 评价假数据生成器。
 * 没有后端，就按店铺 id 稳定地生成一批评价，保证每次进入内容一致。
 */
public class ReviewDataUtils {

    private static final String[] NAMES = {
            "爱吃辣的小王", "今天也要好好吃饭", "橙子汽水", "深夜食堂常客",
            "一只小笼包", "努力减肥中", "不吃香菜", "外卖测评员"
    };

    private static final String[] CONTENTS = {
            "分量很足，包装严实，送到的时候还是热的，下次还点这家！",
            "味道不错，就是稍微有点咸，整体还是很满意的。",
            "配送很快，骑手小哥态度很好，菜品和图片基本一致。",
            "第一次点这家，出乎意料的好吃，性价比很高。",
            "口味偏清淡，适合我这种不能吃辣的人，推荐。",
            "包装有点挤，不过味道没得说，会回购。",
            "出餐速度很快，汤汁没有洒，细节做得很到位。",
            "和朋友一起点的，都说好吃，分量比想象中多。"
    };

    private static final String[] TAGS = {"味道赞", "分量足", "配送快", "包装好", "性价比高"};
    private static final String[] TIMES = {"今天 12:30", "昨天 18:45", "3天前", "上周", "上个月"};

    private ReviewDataUtils() {
    }

    /** 按店铺生成 8 条评价（每次调用顺序稳定） */
    public static List<ReviewBean> build(ShopBean shop) {
        List<ReviewBean> list = new ArrayList<>();
        if (shop == null) {
            return list;
        }
        int seed = Math.abs(shop.getId());
        for (int i = 0; i < 8; i++) {
            String name = NAMES[(seed + i) % NAMES.length];
            String content = CONTENTS[(seed * 3 + i) % CONTENTS.length];
            String tag = TAGS[(seed + i * 2) % TAGS.length];
            String time = TIMES[i % TIMES.length];
            float score = 5.0f - (i % 3) * 0.5f;
            list.add(new ReviewBean(name, score, content, time, tag));
        }
        return list;
    }
}
