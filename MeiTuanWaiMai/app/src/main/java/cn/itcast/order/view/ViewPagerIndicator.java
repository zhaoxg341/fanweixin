package cn.itcast.order.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import cn.itcast.order.R;

/**
 * 自定义控件：广告栏指示器（小圆点）
 * 对应教材 12.4.2 搭建广告栏界面布局
 *
 * 用法：
 *   indicator.setCount(3);            // 依据广告图片数量生成 3 个小圆点
 *   indicator.setCurrentPosition(1);  // 第 2 个圆点变白，其余为半透明灰
 */
public class ViewPagerIndicator extends LinearLayout {

    /** 当前选中的位置 */
    private int currentPosition = 0;

    public ViewPagerIndicator(Context context) {
        this(context, null);
    }

    public ViewPagerIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
    }

    /**
     * 根据广告图片数量生成小圆点
     *
     * @param count 圆点数量
     */
    public void setCount(int count) {
        removeAllViews();
        for (int i = 0; i < count; i++) {
            View point = new View(getContext());
            LayoutParams params = new LayoutParams(dp(6), dp(6));
            params.leftMargin = dp(4);
            params.rightMargin = dp(4);
            point.setLayoutParams(params);
            point.setBackgroundResource(i == currentPosition
                    ? R.drawable.point_selected : R.drawable.point_normal);
            addView(point);
        }
    }

    /**
     * 设置当前选中的圆点
     *
     * @param position 当前页面的下标
     */
    public void setCurrentPosition(int position) {
        if (position < 0 || position >= getChildCount()) {
            return;
        }
        this.currentPosition = position;
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setBackgroundResource(i == position
                    ? R.drawable.point_selected : R.drawable.point_normal);
        }
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    /** dp 转 px，保证不同屏幕密度下小圆点大小一致 */
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
