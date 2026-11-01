package cn.itcast.weixin.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * 微信头像控件。
 * 把任意图片居中裁剪缩放后，按「圆角方形」显示——微信的头像（会话列表、
 * 聊天页、通讯录、朋友圈）都是约 4dp 圆角的方形，不是圆形。
 * 类名保持 CircleImageView 是为了不改几十个引用它的布局。
 */
public class CircleImageView extends ImageView {

    /** 微信头像的圆角比例：约为边长的 9%（52dp 头像 ≈ 4.5dp 圆角） */
    private static final float CORNER_RATIO = 0.09f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private BitmapShader shader;
    private Bitmap bitmap;
    private Drawable lastDrawable;

    public CircleImageView(Context context) {
        super(context);
    }

    public CircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void updateShader() {
        Drawable d = getDrawable();
        if (d == null) {
            return;
        }
        if (d == lastDrawable && shader != null) {
            return;
        }
        lastDrawable = d;
        if (d instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) d).getBitmap();
        } else {
            int w = Math.max(d.getIntrinsicWidth(), 1);
            int h = Math.max(d.getIntrinsicHeight(), 1);
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bitmap);
            d.setBounds(0, 0, w, h);
            d.draw(c);
        }
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getDrawable() == null) {
            super.onDraw(canvas);
            return;
        }
        updateShader();
        if (shader == null || bitmap == null || bitmap.isRecycled()) {
            super.onDraw(canvas);
            return;
        }
        float viewW = getWidth();
        float viewH = getHeight();
        float size = Math.min(viewW, viewH);
        // 居中裁剪缩放
        float scale = size / Math.min(bitmap.getWidth(), bitmap.getHeight());
        matrix.reset();
        matrix.setScale(scale, scale);
        float dx = (viewW - bitmap.getWidth() * scale) / 2f;
        float dy = (viewH - bitmap.getHeight() * scale) / 2f;
        matrix.postTranslate(dx, dy);
        shader.setLocalMatrix(matrix);
        // 微信样式：圆角方形头像
        float corner = size * CORNER_RATIO;
        canvas.drawRoundRect(new android.graphics.RectF(0, 0, viewW, viewH),
                corner, corner, paint);
    }
}
