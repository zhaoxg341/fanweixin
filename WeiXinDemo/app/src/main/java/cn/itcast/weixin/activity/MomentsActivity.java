package cn.itcast.weixin.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.weixin.R;
import cn.itcast.weixin.adapter.MomentAdapter;
import cn.itcast.weixin.bean.MomentBean;
import cn.itcast.weixin.db.MomentDao;
import cn.itcast.weixin.utils.AppPrefs;
import cn.itcast.weixin.utils.AvatarStore;
import cn.itcast.weixin.utils.MomentPhotoStore;
import cn.itcast.weixin.utils.WeixinDataUtils;
import cn.itcast.weixin.view.CircleImageView;

/**
 * 朋友圈：封面 + 动态列表，现在可以真的发动态、点赞、评论和回复。
 *
 * 数据来自 SQLite（MomentDao），第一次进来时会把 assets 里的示例动态导入。
 * 封面依然作为 ListView 的 header，跟着列表一起滚。
 *
 * 交互约定：
 * - 点「评论」= 评论这条动态
 * - 点某条评论 = 回复那个人
 * - 长按某条评论 = 删除（只能删自己的）
 * - 长按动态 = 删除整条（只能删自己的）
 */
public class MomentsActivity extends AppCompatActivity implements MomentAdapter.OnMomentActionListener {

    private static final int REQ_PUBLISH = 3001;

    private MomentAdapter adapter;
    private ListView listMoments;
    private TextView tvMeName;
    private TextView tvEmpty;
    private CircleImageView ivMeAvatar;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moments);

        listMoments = findViewById(R.id.list_moments);
        tvEmpty = findViewById(R.id.tv_empty);

        // header 必须在 setAdapter 之前加，否则不会生效
        View header = LayoutInflater.from(this).inflate(R.layout.moments_header, listMoments, false);
        tvMeName = header.findViewById(R.id.tv_me_name);
        ivMeAvatar = header.findViewById(R.id.iv_me_avatar);
        listMoments.addHeaderView(header);

        adapter = new MomentAdapter(this);
        adapter.setListener(this);
        listMoments.setAdapter(adapter);

        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.iv_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MomentsActivity.this,
                        PublishMomentActivity.class), REQ_PUBLISH);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 首次进入时把 assets 的示例动态搬到数据库（用一个标记位保证只搬一次）
                MomentDao.ensureSeed(MomentsActivity.this);
                final List<MomentBean> moments = MomentDao.queryAll();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        adapter.setData(moments);
                        tvEmpty.setVisibility(moments.isEmpty() ? View.VISIBLE : View.GONE);
                        // 封面右下角用我当前的昵称和头像（可能已经在"我"页面改过）
                        tvMeName.setText(AppPrefs.getMyNick());
                        String avatar = AppPrefs.getMyAvatar();
                        int fallback = WeixinDataUtils.getDrawableId(MomentsActivity.this, avatar);
                        AvatarStore.apply(ivMeAvatar, avatar,
                                fallback == 0 ? R.drawable.avatar_me : fallback);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 发表成功回到本页会先走 onResume，那里已经重新查库，这里不用再刷一次
    }

    // ---------------- 条目交互 ----------------

    @Override
    public void onLike(MomentBean moment) {
        boolean liked = MomentDao.toggleLike(moment.getId());
        Toast.makeText(this, liked ? R.string.moment_liked : R.string.moment_unliked,
                Toast.LENGTH_SHORT).show();
        loadData();
    }

    @Override
    public void onComment(MomentBean moment, MomentBean.CommentBean replyTo) {
        showCommentDialog(moment, replyTo);
    }

    @Override
    public void onDeleteComment(MomentBean moment, MomentBean.CommentBean comment) {
        if (!comment.isMine()) {
            Toast.makeText(this, "只能删除自己的评论", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_comment_title)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MomentDao.deleteComment(comment.getId());
                        Toast.makeText(MomentsActivity.this, R.string.comment_deleted,
                                Toast.LENGTH_SHORT).show();
                        loadData();
                    }
                })
                .show();
    }

    @Override
    public void onImageClick(MomentBean moment, int index) {
        showBigImage(moment, index);
    }

    @Override
    public void onLongPress(final MomentBean moment) {
        if (!moment.isMine()) {
            Toast.makeText(this, "只能删除自己发的动态", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_moment_title)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MomentDao.deleteMoment(moment.getId());
                        // 数据库删了，本机图片文件也一起清掉
                        for (String path : moment.getImages()) {
                            MomentPhotoStore.deleteQuietly(path);
                        }
                        Toast.makeText(MomentsActivity.this, R.string.moment_deleted,
                                Toast.LENGTH_SHORT).show();
                        loadData();
                    }
                })
                .show();
    }

    // ---------------- 弹窗 ----------------

    /** 底部评论框；replyTo 非空时提示「回复 xxx」 */
    private void showCommentDialog(final MomentBean moment, final MomentBean.CommentBean replyTo) {
        final Dialog dialog = new Dialog(this, R.style.BottomSheetDialog);
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_comment, null);
        dialog.setContentView(content);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        TextView hint = content.findViewById(R.id.tv_reply_hint);
        if (replyTo != null) {
            hint.setVisibility(View.VISIBLE);
            hint.setText(getString(R.string.comment_reply_to,
                    replyTo.isMine() ? "我" : replyTo.getUser()));
        }

        final EditText input = content.findViewById(R.id.et_comment);
        TextView send = content.findViewById(R.id.tv_send_comment);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = input.getText().toString().trim();
                if (text.isEmpty()) {
                    return;
                }
                // 回复自己就当作继续评论（不然会显示「我 回复 我」）
                String target = null;
                if (replyTo != null && !replyTo.isMine()) {
                    target = replyTo.getUser();
                }
                MomentDao.addComment(moment.getId(), AppPrefs.getMyNick(), target, text);
                dialog.dismiss();
                loadData();
            }
        });

        dialog.show();
        input.requestFocus();
    }

    /**
     * 点图片看大图：交给 PhotoViewerActivity。
     * 把整条动态的图片一起传过去，这样在查看器里左右滑动就能翻完九张。
     */
    private void showBigImage(MomentBean moment, int index) {
        if (moment.getImages() == null || moment.getImages().isEmpty()) {
            return;
        }
        Intent intent = new Intent(this, PhotoViewerActivity.class);
        intent.putStringArrayListExtra(PhotoViewerActivity.EXTRA_IMAGES,
                new ArrayList<>(moment.getImages()));
        intent.putExtra(PhotoViewerActivity.EXTRA_INDEX,
                Math.max(0, Math.min(index, moment.getImages().size() - 1)));
        startActivity(intent);
    }
}
