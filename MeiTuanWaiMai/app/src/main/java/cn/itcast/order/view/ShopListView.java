package cn.itcast.order.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * 自定义控件：可完整展开的 ListView（对应教材 3.3 自定义控件）
 *
 * 问题：ListView 放在 ScrollView 中时，高度只按“一屏”计算，会出现
 *       只显示一个条目、或者列表内部再滚动一次的冲突现象。
 * 解决：重写 onMeasure()，用一个很大的 AT_MOST 模式重新测量高度，
 *       让 ListView 把自己的真实高度撑开，滚动交给外层 ScrollView。
 */
public class ShopListView extends ListView {

    public ShopListView(Context context) {
        super(context);
    }

    public ShopListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ShopListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }
}
