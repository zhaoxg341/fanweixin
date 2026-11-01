package cn.itcast.weixin.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * 可缩放的大图控件，朋友圈和聊天里的图片都用它看大图。
 *
 * 三种手势各管一件事，互不打架：
 * - 双指捏合：缩放，最大放到适应屏幕的 3 倍
 * - 单指拖动：只在放大后生效，拖出边界会自动弹回
 * - 双击：在 1 倍和 2.5 倍之间切换
 * - 没放大时左右快速滑动：交给外面的查看器翻页
 *
 * 缩放用的是 ImageView 自己的 matrix（scaleType 设成 MATRIX），
 * 没有引第三方库，整个类就两百来行。
 */
public class ZoomImageView extends AppCompatImageView {

    /** 翻页 / 单击的回调，由 PhotoViewerActivity 接住 */
    public interface OnGestureListener {
        void onSwipeLeft();

        void onSwipeRight();

        void onSingleTap();
    }

    /** 相对适应尺寸的最大放大倍数 */
    private static final float MAX_ZOOM = 3f;
    /** 双击放大的倍数 */
    private static final float DOUBLE_TAP_ZOOM = 2.5f;
    /** 判定翻页的最小速度（像素/秒） */
    private static final float FLING_VELOCITY = 600f;

    private final Matrix matrix = new Matrix();
    private final Matrix baseMatrix = new Matrix();
    private final float[] values = new float[9];
    private final RectF drawRect = new RectF();

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private OnGestureListener listener;
    private boolean baseReady;
    /** 构造还没跑完时 setImageDrawable 会被父类提前调用，用它挡一下 */
    private boolean inited;

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
        inited = true;
    }

    public void setOnGestureListener(OnGestureListener listener) {
        this.listener = listener;
    }

    // ---------------- 尺寸与基准矩阵 ----------------

    /**
     * 算出「整张图刚好装进控件」的矩阵，它是所有缩放的原点。
     * 图片比控件小而居中显示，比控件大就等比缩小。
     */
    private void configureBaseMatrix() {
        Drawable drawable = getDrawable();
        if (drawable == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        int dw = drawable.getIntrinsicWidth();
        int dh = drawable.getIntrinsicHeight();
        if (dw <= 0 || dh <= 0) {
            return;
        }
        float scale = Math.min(getWidth() / (float) dw, getHeight() / (float) dh);
        baseMatrix.reset();
        baseMatrix.postScale(scale, scale);
        baseMatrix.postTranslate((getWidth() - dw * scale) / 2f, (getHeight() - dh * scale) / 2f);
        matrix.set(baseMatrix);
        setImageMatrix(matrix);
        baseReady = true;
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        baseReady = false;
        if (inited) {
            configureBaseMatrix();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        configureBaseMatrix();
    }

    private float currentScale() {
        matrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    private float baseScale() {
        baseMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    /** 把图片拖回可视区域：小于控件的方向居中，大于控件的方向不留空白 */
    private void clampMatrix() {
        Drawable drawable = getDrawable();
        if (drawable == null || !baseReady) {
            return;
        }
        drawRect.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        matrix.mapRect(drawRect);

        float dx = 0;
        float dy = 0;
        if (drawRect.width() <= getWidth()) {
            dx = getWidth() / 2f - drawRect.centerX();
        } else if (drawRect.left > 0) {
            dx = -drawRect.left;
        } else if (drawRect.right < getWidth()) {
            dx = getWidth() - drawRect.right;
        }
        if (drawRect.height() <= getHeight()) {
            dy = getHeight() / 2f - drawRect.centerY();
        } else if (drawRect.top > 0) {
            dy = -drawRect.top;
        } else if (drawRect.bottom < getHeight()) {
            dy = getHeight() - drawRect.bottom;
        }
        if (dx != 0 || dy != 0) {
            matrix.postTranslate(dx, dy);
            setImageMatrix(matrix);
        }
    }

    // ---------------- 触摸 ----------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!inited) {
            return super.onTouchEvent(event);
        }
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            clampMatrix();
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (!baseReady) {
                return true;
            }
            float current = currentScale();
            if (current <= 0) {
                return true;
            }
            float min = baseScale();
            float factor = detector.getScaleFactor();
            float target = current * factor;
            if (target < min) {
                factor = min / current;
            } else if (target > min * MAX_ZOOM) {
                factor = min * MAX_ZOOM / current;
            }
            matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
            setImageMatrix(matrix);
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            clampMatrix();
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // 放到 1 倍以上才允许拖动，否则手指一划就把图推走了
            if (!baseReady || currentScale() <= baseScale() * 1.01f) {
                return false;
            }
            matrix.postTranslate(-distanceX, -distanceY);
            setImageMatrix(matrix);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (!baseReady) {
                return false;
            }
            if (currentScale() > baseScale() * 1.05f) {
                // 已经放大了，双击回到适应屏幕
                matrix.set(baseMatrix);
            } else {
                float factor = baseScale() * DOUBLE_TAP_ZOOM / currentScale();
                matrix.postScale(factor, factor, e.getX(), e.getY());
            }
            setImageMatrix(matrix);
            clampMatrix();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               float velocityX, float velocityY) {
            if (!baseReady || currentScale() > baseScale() * 1.01f) {
                return false;
            }
            if (Math.abs(velocityX) < FLING_VELOCITY
                    || Math.abs(velocityX) < Math.abs(velocityY) * 1.5f) {
                return false;
            }
            if (listener == null) {
                return false;
            }
            // 手指往右滑 = 看上一张，往左滑 = 看下一张
            if (velocityX > 0) {
                listener.onSwipeRight();
            } else {
                listener.onSwipeLeft();
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (listener != null) {
                listener.onSingleTap();
            }
            return true;
        }
    }
}
