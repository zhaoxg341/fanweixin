package cn.itcast.milktea.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * 简易流式布局：子 View 排满一行自动换行。
 *
 * 为什么需要它：
 *   糖度有 5 个选项，加料有 4 个选项，直接用 LinearLayout 横排会在小屏手机上被裁掉；
 *   HorizontalScrollView 又要用户左右滑，不如自动换行直观。
 *
 * 实现思路（自定义 ViewGroup 的标准两步）：
 *   onMeasure：先量每个孩子，按可用宽度累加，超宽就换行，算出总高度；
 *   onLayout：再按同样的规则把孩子一个个摆到对应坐标。
 */
public class FlowLayout extends ViewGroup {

    public FlowLayout(Context context) {
        super(context);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);

        int lineWidth = 0;
        int lineHeight = 0;
        int totalHeight = 0;
        int maxLineWidth = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            // 放不下就换行（+1 是为了避免刚好贴边时抖动）
            if (lineWidth + childWidth > widthSize && lineWidth > 0) {
                maxLineWidth = Math.max(maxLineWidth, lineWidth);
                totalHeight += lineHeight;
                lineWidth = 0;
                lineHeight = 0;
            }
            lineWidth += childWidth;
            lineHeight = Math.max(lineHeight, childHeight);
        }
        maxLineWidth = Math.max(maxLineWidth, lineWidth);
        totalHeight += lineHeight;

        int finalWidth = widthMode == MeasureSpec.EXACTLY ? widthSize : maxLineWidth;
        setMeasuredDimension(finalWidth, totalHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int widthSize = r - l;
        int x = 0;
        int y = 0;
        int lineHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (x + childWidth > widthSize && x > 0) {
                x = 0;
                y += lineHeight;
                lineHeight = 0;
            }
            child.layout(x, y, x + childWidth, y + childHeight);
            x += childWidth;
            lineHeight = Math.max(lineHeight, childHeight);
        }
    }
}
