package cn.itcast.weixin.activity;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.weixin.R;
import cn.itcast.weixin.bean.MomentBean;
import cn.itcast.weixin.db.MomentDao;
import cn.itcast.weixin.utils.AppPrefs;
import cn.itcast.weixin.utils.MomentPhotoStore;
import cn.itcast.weixin.view.FlowLayout;

/**
 * 发动态：一段文字 + 最多 9 张相册图片。
 *
 * 图片选中后立刻导入到 App 私有目录（压缩到最长边 1080），
 * 这样即使原图后来被用户删掉，动态里的图还在；发表时数据库里只存文件路径。
 *
 * 选图用 ACTION_GET_CONTENT + EXTRA_ALLOW_MULTIPLE：
 * 系统相册只把用户点选的那几张交给 App，不需要申请读取相册权限。
 */
public class PublishMomentActivity extends AppCompatActivity {

    private static final int REQ_PICK_PHOTO = 2001;

    private EditText etContent;
    private FlowLayout flImages;
    private TextView tvCount;

    private final List<String> images = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    /** 是否已经发表，决定退出时要不要回收导入的图片文件 */
    private boolean published;

    private int gridSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_moment);

        etContent = findViewById(R.id.et_content);
        flImages = findViewById(R.id.fl_images);
        tvCount = findViewById(R.id.tv_count);

        int available = getResources().getDisplayMetrics().widthPixels - dp(32 + 12);
        gridSize = Math.max(dp(64), available / 3);

        findViewById(R.id.tv_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.tv_publish).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publish();
            }
        });

        renderImages();
    }

    /** 画出已选图片 + 末尾的「+」，还没到 9 张才显示加号 */
    private void renderImages() {
        flImages.removeAllViews();
        flImages.setSpacing(dp(6), dp(6));

        for (int i = 0; i < images.size(); i++) {
            final int index = i;
            flImages.addView(buildImageCell(images.get(i), index));
        }
        if (images.size() < 9) {
            flImages.addView(buildAddCell());
        }

        tvCount.setVisibility(images.isEmpty() ? View.GONE : View.VISIBLE);
        tvCount.setText(getString(R.string.moment_photo_count, images.size()));
    }

    private View buildImageCell(String path, final int index) {
        FrameLayout cell = new FrameLayout(this);
        cell.setLayoutParams(new ViewGroup.LayoutParams(gridSize, gridSize));

        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap bitmap = MomentPhotoStore.load(path);
        if (bitmap != null) {
            iv.setImageBitmap(bitmap);
        }
        cell.addView(iv);

        // 右上角的删除按钮
        ImageView close = new ImageView(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(20), dp(20));
        lp.gravity = android.view.Gravity.END | android.view.Gravity.TOP;
        lp.topMargin = dp(3);
        lp.rightMargin = dp(3);
        close.setLayoutParams(lp);
        close.setBackgroundResource(R.drawable.bg_circle_dark);
        close.setPadding(dp(4), dp(4), dp(4), dp(4));
        close.setImageResource(R.drawable.ic_close_small);
        close.setContentDescription(getString(R.string.delete));
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String removed = images.remove(index);
                // 还没发表，这张图已经从列表里删了，文件也顺手清掉
                MomentPhotoStore.deleteQuietly(removed);
                renderImages();
            }
        });
        cell.addView(close);
        return cell;
    }

    private View buildAddCell() {
        FrameLayout cell = new FrameLayout(this);
        cell.setLayoutParams(new ViewGroup.LayoutParams(gridSize, gridSize));
        cell.setBackgroundResource(R.drawable.bg_photo_add);
        cell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickPhotos();
            }
        });

        ImageView plus = new ImageView(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(26), dp(26));
        lp.gravity = android.view.Gravity.CENTER;
        plus.setLayoutParams(lp);
        plus.setImageResource(R.drawable.ic_plus);
        plus.setColorFilter(getResources().getColor(R.color.text_hint));
        cell.addView(plus);
        return cell;
    }

    /** 打开系统相册，允许一次选多张 */
    private void pickPhotos() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
            startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.add_photo)), REQ_PICK_PHOTO);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.moment_photo_none, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_PHOTO || resultCode != RESULT_OK || data == null) {
            return;
        }

        // 多选时结果在 ClipData 里，单选时在 getData() 里
        final List<Uri> picked = new ArrayList<>();
        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                picked.add(clip.getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            picked.add(data.getData());
        }
        if (picked.isEmpty()) {
            return;
        }

        final int remain = 9 - images.size();
        if (remain <= 0) {
            Toast.makeText(this, R.string.moment_photo_limit, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.moment_photo_processing, Toast.LENGTH_SHORT).show();

        // 解码 + 压缩放后台，主线程只负责最后刷新界面
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<String> added = new ArrayList<>();
                int count = Math.min(remain, picked.size());
                for (int i = 0; i < count; i++) {
                    String path = MomentPhotoStore.importFromUri(
                            getApplicationContext(), picked.get(i));
                    if (path != null) {
                        added.add(path);
                    }
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (added.isEmpty()) {
                            Toast.makeText(PublishMomentActivity.this,
                                    R.string.moment_photo_failed, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (picked.size() > count) {
                            Toast.makeText(PublishMomentActivity.this,
                                    R.string.moment_photo_limit, Toast.LENGTH_SHORT).show();
                        }
                        images.addAll(added);
                        renderImages();
                    }
                });
            }
        }).start();
    }

    private void publish() {
        String content = etContent.getText().toString().trim();
        if (content.isEmpty() && images.isEmpty()) {
            Toast.makeText(this, R.string.moment_publish_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        MomentBean moment = new MomentBean();
        moment.setName(AppPrefs.getMyNick());
        moment.setAvatar(AppPrefs.getMyAvatar());
        moment.setContent(content);
        moment.setImages(new ArrayList<>(images));
        moment.setTs(System.currentTimeMillis());
        moment.setTime(getString(R.string.moment_just_now));
        moment.setMine(true);
        MomentDao.insert(moment);
        published = true;

        Toast.makeText(this, R.string.moment_published, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
        // 没点「发表」就退出的，把已经导入的图片清掉，别留垃圾文件
        if (isFinishing() && !published) {
            for (String path : images) {
                MomentPhotoStore.deleteQuietly(path);
            }
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
