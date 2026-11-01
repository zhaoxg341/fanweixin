package cn.itcast.weixin.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.weixin.R;
import cn.itcast.weixin.utils.MomentPhotoStore;
import cn.itcast.weixin.utils.WeixinDataUtils;
import cn.itcast.weixin.view.ZoomImageView;

/**
 * 全屏看大图，朋友圈九宫格和聊天里的图片消息都跳到这里。
 *
 * 传入 images 数组和起始位置，就能左右滑动翻页；
 * 图片本身支持双指缩放、双击放大，单击隐藏/显示顶栏。
 * 只有一张图时自动隐藏页码，滑动手势也不会翻出边界。
 */
public class PhotoViewerActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGES = "images";
    public static final String EXTRA_INDEX = "index";

    private final List<String> images = new ArrayList<>();
    private int index;

    private ZoomImageView imageView;
    private TextView tvCounter;
    private View topBar;
    private View hint;
    private boolean barsVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);

        imageView = findViewById(R.id.iv_photo);
        tvCounter = findViewById(R.id.tv_counter);
        topBar = findViewById(R.id.ll_top_bar);
        hint = findViewById(R.id.tv_hint);

        ArrayList<String> extra = getIntent().getStringArrayListExtra(EXTRA_IMAGES);
        if (extra != null) {
            images.addAll(extra);
        }
        if (images.isEmpty()) {
            finish();
            return;
        }
        index = Math.max(0, Math.min(getIntent().getIntExtra(EXTRA_INDEX, 0), images.size() - 1));

        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        imageView.setOnGestureListener(new ZoomImageView.OnGestureListener() {
            @Override
            public void onSwipeLeft() {
                showAt(index + 1);
            }

            @Override
            public void onSwipeRight() {
                showAt(index - 1);
            }

            @Override
            public void onSingleTap() {
                toggleBars();
            }
        });

        showAt(index);
    }

    /** 切到第 which 张，越界就忽略（滑到头了自然不动） */
    private void showAt(int which) {
        if (which < 0 || which >= images.size()) {
            return;
        }
        index = which;
        String path = images.get(index);

        if (MomentPhotoStore.isLocalFile(path)) {
            Bitmap bitmap = MomentPhotoStore.loadFull(path);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageDrawable(null);
            }
        } else {
            int resId = WeixinDataUtils.getDrawableId(this, path);
            if (resId != 0) {
                imageView.setImageResource(resId);
            } else {
                imageView.setImageDrawable(null);
            }
        }

        tvCounter.setText(getString(R.string.photo_counter, index + 1, images.size()));
        tvCounter.setVisibility(images.size() > 1 ? View.VISIBLE : View.GONE);
    }

    /** 单击图片：把顶栏和提示淡出，留一张干净的大图 */
    private void toggleBars() {
        barsVisible = !barsVisible;
        float from = barsVisible ? 0f : 1f;
        float to = barsVisible ? 1f : 0f;
        AlphaAnimation anim = new AlphaAnimation(from, to);
        anim.setDuration(160);
        anim.setFillAfter(true);

        topBar.startAnimation(anim);
        hint.startAnimation(anim);
        topBar.setVisibility(barsVisible ? View.VISIBLE : View.GONE);
        hint.setVisibility(barsVisible ? View.VISIBLE : View.GONE);
    }
}
