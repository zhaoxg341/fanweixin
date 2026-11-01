package cn.itcast.weixin.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * 自动换行的流式布局，用来排朋友圈的九宫格配图。
 * 子 View 按自身宽度依次排列，一行放不下就换到下一行。
 */
public class FlowLayout extends ViewGroup {

    private int horizontalSpacing = 0;
    private int verticalSpacing = 0;

    public FlowLayout(Context context) {
        super(context);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSpacing(int horizontal, int vertical) {
        horizontalSpacing = horizontal;
        verticalSpacing = vertical;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int usableWidth = widthSize - getPaddingLeft() - getPaddingRight();

        int lineWidth = 0;
        int lineHeight = 0;
        int totalHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (lineWidth + childWidth > usableWidth && lineWidth > 0) {
                totalHeight += lineHeight + verticalSpacing;
                lineWidth = 0;
                lineHeight = 0;
            }
            lineWidth += childWidth + horizontalSpacing;
            lineHeight = Math.max(lineHeight, childHeight);
        }
        totalHeight += lineHeight;

        int height = totalHeight + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(widthSize, resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int usableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int lineHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (left - getPaddingLeft() + childWidth > usableWidth) {
                left = getPaddingLeft();
                top += lineHeight + verticalSpacing;
                lineHeight = 0;
            }
            child.layout(left, top, left + childWidth, top + childHeight);
            left += childWidth + horizontalSpacing;
            lineHeight = Math.max(lineHeight, childHeight);
        }
    }
}
