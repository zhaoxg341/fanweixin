package cn.itcast.weixin.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * 通讯录右侧字母索引栏（A~Z）。
 * 手指按住滑动时回调 onLetterChanged()，联系人列表据此跳转到对应字母分组。
 */
public class SideIndexBar extends View {

    public interface OnLetterChangedListener {
        void onLetterChanged(String letter);
    }

    public static final String[] LETTERS = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z"
    };

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubbleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int currentIndex = -1;
    private OnLetterChangedListener listener;

    public SideIndexBar(Context context) {
        super(context);
        init();
    }

    public SideIndexBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SideIndexBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint.setColor(Color.parseColor("#7F7F7F"));
        textPaint.setTextSize(sp(11));
        textPaint.setTextAlign(Paint.Align.CENTER);
        bubblePaint.setColor(Color.parseColor("#07C160"));
        bubbleTextPaint.setColor(Color.WHITE);
        bubbleTextPaint.setTextSize(sp(12));
        bubbleTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setOnLetterChangedListener(OnLetterChangedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int count = LETTERS.length;
        float cellH = getHeight() / (float) count;
        float x = getWidth() * 0.62f;
        for (int i = 0; i < count; i++) {
            float cy = i * cellH + cellH / 2f;
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float baseline = cy - (fm.ascent + fm.descent) / 2f;
            if (i == currentIndex) {
                // 选中：绿色圆 + 白字
                canvas.drawCircle(x, cy, cellH * 0.52f, bubblePaint);
                Paint.FontMetrics bfm = bubbleTextPaint.getFontMetrics();
                canvas.drawText(LETTERS[i], x, cy - (bfm.ascent + bfm.descent) / 2f, bubbleTextPaint);
            } else {
                canvas.drawText(LETTERS[i], x, baseline, textPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float y = event.getY();
        int index = (int) (y / (getHeight() / (float) LETTERS.length));
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (index >= 0 && index < LETTERS.length) {
                    if (index != currentIndex) {
                        currentIndex = index;
                        invalidate();
                        if (listener != null) {
                            listener.onLetterChanged(LETTERS[index]);
                        }
                    }
                } else {
                    currentIndex = -1;
                    invalidate();
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                currentIndex = -1;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
