package cn.itcast.weixin.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cn.itcast.weixin.R;
import cn.itcast.weixin.adapter.AvatarPickerAdapter;
import cn.itcast.weixin.bean.ChatBean;
import cn.itcast.weixin.db.AvatarDao;
import cn.itcast.weixin.db.RemarkDao;
import cn.itcast.weixin.utils.AvatarStore;
import cn.itcast.weixin.utils.ChatManager;
import cn.itcast.weixin.utils.WeixinDataUtils;
import cn.itcast.weixin.view.CircleImageView;

/**
 * 联系人资料页：头像 + 昵称 + 微信号 + 几行功能项 + 底部"发消息"。
 *
 * 这里有两处是真能编辑的：
 * 1. 「头像」行：弹 16 宫格选内置头像，也可以「从相册选择」自己手机里的照片；
 * 2. 「设置备注和标签」行：弹输入框改名，同样全应用联动。
 *
 * 两者都做了"统一出口"：显示名字走 RemarkDao.displayName()，
 * 显示头像走 AvatarStore.apply()，所以改一处、各处生效。
 */
public class UserInfoActivity extends AppCompatActivity {

    /** 相册选图的结果码 */
    private static final int REQ_PICK_AVATAR = 1001;

    private int contactId = -1;
    private String name;
    private String weixinId;
    /** assets 里原本的头像，用户没自定义时就用它 */
    private String avatarName;

    private TextView tvName;
    private CircleImageView ivAvatar;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        Intent intent = getIntent();
        contactId = intent.getIntExtra("contactId", -1);
        name = safe(intent.getStringExtra("name"));
        weixinId = safe(intent.getStringExtra("weixinId"));
        avatarName = safe(intent.getStringExtra("avatar"));
        String region = safe(intent.getStringExtra("region"));

        initTitleBar();
        tvName = findViewById(R.id.tv_name);
        bindProfile(region);
        bindRows();

        findViewById(R.id.btn_send_message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChat();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从别处改了备注 / 头像再回来，这里要重新读一次
        tvName.setText(RemarkDao.displayName(name));
        refreshAvatar();
    }

    /** 资料页没有标题文字，只留返回和更多 */
    private void initTitleBar() {
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText("");

        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ImageView ivRight = findViewById(R.id.iv_right);
        ivRight.setImageResource(R.drawable.ic_more);
        ivRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(UserInfoActivity.this, R.string.more, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindProfile(String region) {
        ivAvatar = findViewById(R.id.iv_avatar);
        refreshAvatar();
        // 有备注就显示备注
        tvName.setText(RemarkDao.displayName(name));
        ((TextView) findViewById(R.id.tv_wxid)).setText(getString(R.string.wxid_prefix) + weixinId);
        ((TextView) findViewById(R.id.tv_region)).setText(region);
    }

    /**
     * 刷新页面上两处头像：顶部大头像 + 「头像」行的小预览。
     * 都从 AvatarDao 取"该显示哪张"，所以用户在别处改过也会同步。
     */
    private void refreshAvatar() {
        String show = AvatarDao.displayAvatar(name, avatarName);
        int fallback = WeixinDataUtils.getDrawableId(this, show);
        if (fallback == 0) {
            fallback = R.drawable.avatar_default;
        }
        AvatarStore.apply(ivAvatar, show, fallback);
        View row = findViewById(R.id.row_avatar);
        if (row != null) {
            CircleImageView preview = row.findViewById(R.id.iv_preview);
            AvatarStore.apply(preview, show, fallback);
        }
    }

    /** 五行功能项：头像、备注、朋友权限、朋友圈、更多信息 */
    private void bindRows() {
        bindRow(R.id.row_remark, R.drawable.ic_tag, getString(R.string.remark));
        bindRow(R.id.row_permission, R.drawable.ic_settings, getString(R.string.friend_permission));
        bindRow(R.id.row_moments, R.drawable.ic_album, getString(R.string.album));
        bindRow(R.id.row_more, R.drawable.ic_more, getString(R.string.more_info));

        // 「头像」行：弹 16 宫格选头像（它的布局和别行不一样，所以不走 bindRow）
        findViewById(R.id.row_avatar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAvatarPicker();
            }
        });

        // 只有「备注」改成真编辑，其余保持原来的提示
        findViewById(R.id.row_remark).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditRemarkDialog();
            }
        });
    }

    private void bindRow(int rowId, int iconRes, final String title) {
        View row = findViewById(rowId);
        ((ImageView) row.findViewById(R.id.iv_icon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.tv_name)).setText(title);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(UserInfoActivity.this, title, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** 改备注：输入框里预填当前备注，留空表示删除备注 */
    private void showEditRemarkDialog() {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_edit, null);
        final EditText input = content.findViewById(R.id.et_dialog_input);
        input.setHint(R.string.edit_remark_hint);
        String current = RemarkDao.getByName(name);
        input.setText(current == null ? "" : current);
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.edit_remark_title)
                .setView(content)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RemarkDao.save(contactId, name, input.getText().toString());
                        if (tvName != null) {
                            tvName.setText(RemarkDao.displayName(name));
                        }
                        Toast.makeText(UserInfoActivity.this, R.string.remark_saved,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    /**
     * 选头像：4 列 16 宫格，点哪张就存哪张，立刻生效。
     * 底部「恢复默认」= 删掉自定义记录，回到 assets 里的原头像。
     */
    private void showAvatarPicker() {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_avatar_picker, null);
        GridView grid = content.findViewById(R.id.gv_avatar);
        final AvatarPickerAdapter pickerAdapter =
                new AvatarPickerAdapter(this, AvatarDao.displayAvatar(name, avatarName));
        grid.setAdapter(pickerAdapter);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_avatar_title)
                .setView(content)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setNeutralButton(R.string.avatar_reset, null)
                .create();

        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                applyAvatar(AvatarDao.POOL[position], false);
                dialog.dismiss();
            }
        });

        dialog.show();

        // 从相册选：关掉宫格对话框，交给系统相册
        content.findViewById(R.id.tv_pick_gallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                pickFromGallery();
            }
        });

        // 覆盖「恢复默认」按钮的默认行为（默认点一下就关），改成先删记录再关
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyAvatar(null, true);
                dialog.dismiss();
            }
        });
    }

    /**
     * 打开系统相册选图。
     * 用 ACTION_GET_CONTENT 而不是 READ_MEDIA_IMAGES 权限：
     * 系统选择器只把我们选中的那一张交给 App，不需要申请存储权限，
     * 也不会要求用户把整个相册都开放给应用。
     */
    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.avatar_from_gallery)), REQ_PICK_AVATAR);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.avatar_pick_none, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_AVATAR || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        final Uri uri = data.getData();
        // 用 Application 的 Context 读图，避免处理过程中 Activity 被回收
        final android.content.Context appContext = getApplicationContext();
        Toast.makeText(this, R.string.avatar_processing, Toast.LENGTH_SHORT).show();

        // 解码 + 压缩可能要几百毫秒，放后台线程，处理完再回主线程刷新界面
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String path = AvatarStore.importFromUri(appContext, uri, name);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (path == null) {
                            Toast.makeText(UserInfoActivity.this, R.string.avatar_pick_failed,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            applyAvatar(path, false);
                        }
                    }
                });
            }
        }).start();
    }

    /** 保存并立刻刷新；avatar 传 null 表示恢复默认 */
    private void applyAvatar(String avatar, boolean reset) {
        AvatarDao.save(name, avatar);
        refreshAvatar();

        // 会话列表 / 聊天页读的是 ChatBean.avatar，同步一下并通知它们重画
        ChatBean chat = ChatManager.findChatByName(name);
        if (chat != null) {
            chat.setAvatar(AvatarDao.displayAvatar(name, avatarName));
            ChatManager.notifyChanged(chat.getId());
        }

        Toast.makeText(this, reset ? R.string.avatar_reset_done : R.string.avatar_saved,
                Toast.LENGTH_SHORT).show();
    }

    /** 找到与联系人同名的会话并跳转；会话数据还没加载好时先加载一次 */
    private void openChat() {
        ChatBean target = ChatManager.findChatByName(name);
        if (target != null) {
            startChat(target);
            return;
        }
        if (ChatManager.isInited()) {
            toastNoChat();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String json = WeixinDataUtils.loadWeixinJson(UserInfoActivity.this);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ChatManager.init(WeixinDataUtils.parseChatList(json));
                        ChatBean chat = ChatManager.findChatByName(name);
                        if (chat != null) {
                            startChat(chat);
                        } else {
                            toastNoChat();
                        }
                    }
                });
            }
        }).start();
    }

    private void startChat(ChatBean chat) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatId", chat.getId());
        intent.putExtra("chatName", chat.getName());
        intent.putExtra("chatAvatar", chat.getAvatar());
        startActivity(intent);
    }

    private void toastNoChat() {
        Toast.makeText(this, "还没有和「" + name + "」的聊天记录", Toast.LENGTH_SHORT).show();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
