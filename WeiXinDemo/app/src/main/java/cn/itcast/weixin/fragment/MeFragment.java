package cn.itcast.weixin.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import cn.itcast.weixin.R;
import cn.itcast.weixin.activity.FavoriteActivity;
import cn.itcast.weixin.activity.LoginActivity;
import cn.itcast.weixin.activity.SettingsActivity;
import cn.itcast.weixin.utils.AppPrefs;
import cn.itcast.weixin.utils.AvatarStore;
import cn.itcast.weixin.utils.ChatClient;
import cn.itcast.weixin.utils.WeixinDataUtils;
import cn.itcast.weixin.view.CircleImageView;

/**
 * 我 Tab：顶部个人卡片 + 服务 / 收藏 / 朋友圈 / 卡包 / 表情 / 设置。
 *
 * 昵称和头像都能改：
 * 点上面的个人卡片 → 弹出「修改昵称 / 从相册更换头像 / 恢复默认头像」。
 * 昵称存 SharedPreferences；头像会先把相册图片压到 512x512 存进 App 私有目录，
 * 再把这个路径存进 SharedPreferences，所以重启 App 也还在。
 * 朋友圈封面、聊天右侧气泡、局域网聊天身份都读同一份数据，改一次全变。
 */
public class MeFragment extends Fragment {

    /** 相册选图的结果码 */
    private static final int REQ_PICK_AVATAR = 2001;

    private CircleImageView ivAvatar;
    private TextView tvNick;
    private TextView tvWxid;
    private TextView tvServerState;

    /** 服务器连接状态变化时刷新卡片上的小字 */
    private final ChatClient.Listener clientListener = new ChatClient.Listener() {
        @Override
        public void onConnectionChanged(boolean connected, String hint) {
            refreshServerState();
        }

        @Override
        public void onNotice(String message) {
            // 通知由 MainActivity 统一弹，这里不用管
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_me, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ivAvatar = view.findViewById(R.id.iv_me_avatar);
        tvNick = view.findViewById(R.id.tv_nick);
        tvWxid = view.findViewById(R.id.tv_wxid);
        tvServerState = view.findViewById(R.id.tv_server_state);

        view.findViewById(R.id.rl_profile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProfileMenu();
            }
        });

        bindRow(view, R.id.row_service, R.drawable.ic_pay, getString(R.string.service));
        bindRow(view, R.id.row_favorite, R.drawable.ic_fav, getString(R.string.favorite));
        bindRow(view, R.id.row_album, R.drawable.ic_album, getString(R.string.album));
        bindRow(view, R.id.row_card, R.drawable.ic_card, getString(R.string.card));
        bindRow(view, R.id.row_emoji, R.drawable.ic_emoji, getString(R.string.emoji));
        bindRow(view, R.id.row_settings, R.drawable.ic_settings, getString(R.string.settings));

        // 「收藏」：打开收藏列表页（长按聊天消息收藏的内容）
        view.findViewById(R.id.row_favorite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), FavoriteActivity.class));
            }
        });

        // 「设置」：打开设置页（改昵称/微信号/性别/地区/服务器，退出登录）
        view.findViewById(R.id.row_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshProfile();
        refreshServerState();
        ChatClient.get().addListener(clientListener);
    }

    @Override
    public void onPause() {
        ChatClient.get().removeListener(clientListener);
        super.onPause();
    }

    /** 给一行"图标 + 文字"赋值并加上点击提示 */
    private void bindRow(View root, int rowId, int iconRes, final String title) {
        View row = root.findViewById(rowId);
        ImageView ivIcon = row.findViewById(R.id.iv_icon);
        TextView tvName = row.findViewById(R.id.tv_name);
        ivIcon.setImageResource(iconRes);
        tvName.setText(title);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), title, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** 昵称和头像都从 AppPrefs 取（没改过就回落到 JSON 里的默认值） */
    private void refreshProfile() {
        if (tvNick == null) {
            return;
        }
        tvNick.setText(AppPrefs.getMyNick());
        tvWxid.setText(getString(R.string.wxid_prefix) + AppPrefs.getMyWxId());

        // 头像可能是 drawable 资源名，也可能是相册导入的本地路径，统一交给 AvatarStore
        String avatar = AppPrefs.getMyAvatar();
        int fallback = WeixinDataUtils.getDrawableId(getActivity(), avatar);
        if (fallback == 0) {
            fallback = R.drawable.avatar_me;
        }
        AvatarStore.apply(ivAvatar, avatar, fallback);
    }

    /** 点个人卡片：改昵称 / 从相册换头像 / 恢复默认头像 */
    private void showProfileMenu() {
        final String[] items = {
                getString(R.string.menu_edit_nick),
                getString(R.string.menu_edit_avatar),
                getString(R.string.avatar_reset)
        };
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.my_profile)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            showEditNickDialog();
                        } else if (which == 1) {
                            pickAvatarFromGallery();
                        } else {
                            resetMyAvatar();
                        }
                    }
                })
                .show();
    }

    /** 打开系统相册；只需要用户选中的那一张，不用申请相册权限 */
    private void pickAvatarFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.avatar_from_gallery)), REQ_PICK_AVATAR);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), R.string.avatar_pick_none, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_AVATAR || resultCode != Activity.RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        if (getActivity() == null) {
            return;
        }
        final Uri uri = data.getData();
        // 用 Application 的 Context 读图：即使这期间用户切走了页面，也不会因为 Activity 被回收而崩
        final android.content.Context appContext = getActivity().getApplicationContext();
        Toast.makeText(getActivity(), R.string.avatar_processing, Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String path = AvatarStore.importFromUri(appContext, uri,
                        AppPrefs.getMyNick());
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded() || getActivity() == null) {
                            return;
                        }
                        if (path == null) {
                            Toast.makeText(getActivity(), R.string.avatar_pick_failed,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // 先删掉上一张自己导入的图，再保存新路径
                        AvatarStore.deleteIfLocal(AppPrefs.getMyAvatar());
                        AppPrefs.setMyAvatar(path);
                        refreshProfile();
                        Toast.makeText(getActivity(), R.string.avatar_saved,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /** 恢复默认：清掉路径，getMyAvatar() 会自动回落到 assets 里的原头像 */
    private void resetMyAvatar() {
        AvatarStore.deleteIfLocal(AppPrefs.getMyAvatar());
        AppPrefs.setMyAvatar("");
        refreshProfile();
        Toast.makeText(getActivity(), R.string.avatar_reset_done, Toast.LENGTH_SHORT).show();
    }

    /** 卡片上第三行小字：当前是本地演示，还是连着服务器 */
    private void refreshServerState() {
        if (tvServerState == null) {
            return;
        }
        String address = AppPrefs.getServerHost() + ":" + AppPrefs.getServerPort();
        if (!AppPrefs.isLanMode()) {
            tvServerState.setText("本地演示模式");
            tvServerState.setTextColor(ContextCompat.getColor(getActivity(), R.color.text_hint));
        } else if (ChatClient.get().isConnected()) {
            tvServerState.setText("已连接 " + address);
            tvServerState.setTextColor(ContextCompat.getColor(getActivity(), R.color.weixin_green));
        } else {
            tvServerState.setText("未连接 " + address + "，正在重试");
            tvServerState.setTextColor(ContextCompat.getColor(getActivity(), R.color.badge_red));
        }
    }

    /** 点个人卡片：改我的昵称 */
    private void showEditNickDialog() {
        View content = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit, null);
        final EditText input = content.findViewById(R.id.et_dialog_input);
        input.setText(AppPrefs.getMyNick());
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.edit_nick_title)
                .setView(content)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String nick = input.getText().toString().trim();
                        if (nick.isEmpty()) {
                            Toast.makeText(getActivity(), R.string.nick_empty,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        AppPrefs.setMyNick(nick);
                        // 服务器是按昵称认人的，改名之后要重新登录一次
                        ChatClient.get().disconnect();
                        ChatClient.get().connectIfNeeded();
                        refreshProfile();
                        refreshServerState();
                        Toast.makeText(getActivity(), R.string.nick_saved,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    /** 「设置」：改昵称/服务器，或者退出登录 */
    private void showSettingsDialog() {
        final String[] items = {
                getString(R.string.login_settings),
                getString(R.string.login_logout)
        };
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.settings)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            intent.putExtra("edit", true);
                            startActivity(intent);
                        } else {
                            logout();
                        }
                    }
                })
                .show();
    }

    private void logout() {
        AppPrefs.logout();
        ChatClient.get().disconnect();
        Toast.makeText(getActivity(), R.string.logout_done, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}
