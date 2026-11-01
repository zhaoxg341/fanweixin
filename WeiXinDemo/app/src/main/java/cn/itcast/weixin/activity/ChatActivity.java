package cn.itcast.weixin.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cn.itcast.weixin.R;
import cn.itcast.weixin.adapter.MsgAdapter;
import cn.itcast.weixin.bean.FavoriteBean;
import cn.itcast.weixin.bean.MsgBean;
import cn.itcast.weixin.db.AvatarDao;
import cn.itcast.weixin.db.FavoriteDao;
import cn.itcast.weixin.db.RemarkDao;
import cn.itcast.weixin.utils.AppPrefs;
import cn.itcast.weixin.utils.AutoReplyRules;
import cn.itcast.weixin.utils.ChatClient;
import cn.itcast.weixin.utils.ChatManager;
import cn.itcast.weixin.utils.Constant;
import cn.itcast.weixin.utils.MomentPhotoStore;
import cn.itcast.weixin.utils.WeixinDataUtils;

/**
 * 聊天界面：消息列表 + 底部输入栏。
 *
 * 消息本身不存在这里，而是统一放在 ChatManager 里，
 * 所以发完消息返回微信 Tab 时，会话的最后一条消息和红点都会自动跟着变。
 */
public class ChatActivity extends AppCompatActivity {

    /** 模拟对方自动回复的几句话，轮着用 */
    private static final String[] AUTO_REPLIES = {
            "收到，稍等我看一下",
            "好的，没问题～",
            "哈哈哈，可以可以",
            "嗯嗯，那到时候联系",
            "在忙，一会儿回你"
    };

    /** 从相册选一张图发出去 */
    private static final int REQ_PICK_IMAGE = 4101;

    private int chatId;
    private String chatName;
    private String chatAvatar;

    private ListView listMsg;
    private MsgAdapter adapter;
    private EditText etInput;
    private TextView tvSend;
    private TextView tvHoldTalk;
    private TextView tvTalkOverlay;
    private ImageView ivEmoji;
    private ImageView ivAdd;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private int replyIndex = 0;
    /** 命中了关键词规则时，要回的那句话；为 null 表示走随机回复 */
    private String pendingReply;
    /** 语音模式下按住说话的开始时间，松开时算时长 */
    private long talkStartAt;
    /** 聊到「红包」时，对方要回一个红包 */
    private boolean pendingFriendRedPacket;

    /** 重入保护：clearUnread() 也会触发回调，避免自己调自己 */
    private boolean refreshing = false;

    /** 当前会话有变化（自己发的、对方发来的）就刷新列表 */
    private final ChatManager.OnChatChangedListener chatListener =
            new ChatManager.OnChatChangedListener() {
                @Override
                public void onChatChanged(int changedChatId) {
                    if (changedChatId != chatId || refreshing) {
                        return;
                    }
                    refreshing = true;
                    try {
                        // 人就在这个聊天页里，进来的消息直接算已读
                        ChatManager.clearUnread(chatId);
                        refreshMessages();
                        scrollToBottom();
                    } finally {
                        refreshing = false;
                    }
                }
            };

    /** 延迟自动回复的任务，页面销毁前要记得取消 */
    private final Runnable autoReplyTask = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (pendingFriendRedPacket) {
                pendingFriendRedPacket = false;
                // 聊到「红包」：对方回一个随机金额的红包
                String[] greetings = {"恭喜发财", "大吉大利", "请你的", "一点心意"};
                double amount = 1 + random.nextDouble() * 18;
                ChatManager.receiveRedPacket(chatId,
                        greetings[random.nextInt(greetings.length)],
                        String.format(java.util.Locale.CHINA, "%.2f", amount));
                refreshMessages();
                scrollToBottom();
                return;
            }
            // 命中关键词就用规则的回复，否则轮着用默认那几句
            String reply;
            if (pendingReply != null) {
                reply = pendingReply;
                pendingReply = null;
            } else {
                reply = AUTO_REPLIES[replyIndex % AUTO_REPLIES.length];
                replyIndex++;
            }
            ChatManager.receive(chatId, reply);
            refreshMessages();
            scrollToBottom();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatId = getIntent().getIntExtra("chatId", -1);
        chatName = getIntent().getStringExtra("chatName");
        chatAvatar = getIntent().getStringExtra("chatAvatar");

        initTitleBar();
        initMessageList();
        initInputBar();

        // 打开聊天页就算已读，红点清掉
        ChatManager.clearUnread(chatId);
        ChatManager.addListener(chatListener);
        ensureData();
    }

    /** 标题栏：返回 + 对方昵称 + 更多 */
    private void initTitleBar() {
        TextView tvTitle = findViewById(R.id.tv_title);
        // 备注优先：给这个人加过备注，标题栏就显示备注
        tvTitle.setText(chatName == null ? "" : RemarkDao.displayName(chatName));

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
                showChatMenu();
            }
        });
    }

    /** 右上角「⋯」：看资料 / 清空记录，都是真能用的 */
    private void showChatMenu() {
        String[] items = {
                getString(R.string.chat_menu_profile),
                getString(R.string.chat_menu_clear)
        };
        new AlertDialog.Builder(this)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            openProfile();
                        } else {
                            confirmClearChat();
                        }
                    }
                })
                .show();
    }

    private void openProfile() {
        Intent intent = new Intent(this, UserInfoActivity.class);
        intent.putExtra("name", chatName);
        intent.putExtra("avatar", chatAvatar);
        startActivity(intent);
    }

    private void confirmClearChat() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.chat_clear_title,
                        chatName == null ? "" : RemarkDao.displayName(chatName)))
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.chat_clear_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 还没执行的那条模拟回复也一并取消，不然清完又冒出来一条
                        handler.removeCallbacks(autoReplyTask);
                        pendingReply = null;
                        ChatManager.clearMessages(chatId);
                        refreshMessages();
                        Toast.makeText(ChatActivity.this, R.string.chat_cleared,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void initMessageList() {
        listMsg = findViewById(R.id.list_msg);
        // 左侧用对方的头像（可能被改过，走 AvatarDao 统一出口），右侧用我自己的头像
        adapter = new MsgAdapter(this, chatName,
                AvatarDao.displayAvatar(chatName, chatAvatar), AppPrefs.getMyAvatar());
        adapter.setActionListener(new MsgAdapter.OnMsgAction() {
            @Override
            public void onRedPacketClick(MsgBean msg) {
                showRedPacketOpenDialog(msg);
            }

            @Override
            public void onTransferClick(MsgBean msg) {
                onTransferCardClick(msg);
            }

            @Override
            public void onAvatarDoubleTap(MsgBean msg) {
                doPat(msg.isFromMe());
            }

            @Override
            public boolean onMsgLongClick(MsgBean msg) {
                showMsgMenu(msg);
                return true;
            }
        });
        listMsg.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 在资料页换过对方头像、或在"我"页面换过自己的头像，回到这里气泡旁边也要跟着换
        if (adapter != null) {
            adapter.setOtherAvatar(AvatarDao.displayAvatar(chatName, chatAvatar));
            adapter.setMyAvatar(AppPrefs.getMyAvatar());
        }
    }

    private void initInputBar() {
        etInput = findViewById(R.id.et_input);
        tvSend = findViewById(R.id.tv_send);
        tvHoldTalk = findViewById(R.id.tv_hold_talk);
        tvTalkOverlay = findViewById(R.id.tv_talk_overlay);
        ivEmoji = findViewById(R.id.iv_emoji);
        ivAdd = findViewById(R.id.iv_add);

        // 有内容才显示“发送”，同时把表情、加号收起来
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean hasText = s != null && s.toString().trim().length() > 0;
                tvSend.setVisibility(hasText ? View.VISIBLE : View.GONE);
                ivEmoji.setVisibility(hasText ? View.GONE : View.VISIBLE);
                ivAdd.setVisibility(hasText ? View.GONE : View.VISIBLE);
            }
        });

        // 软键盘上直接点“发送”也能发出去
        etInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        etInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    sendMessage();
                    return true;
                }
                return false;
            }
        });

        tvSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        View.OnClickListener stub = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ChatActivity.this, R.string.input_hint, Toast.LENGTH_SHORT).show();
            }
        };
        // 左下角喇叭：切换「文字输入 / 按住说话」两种模式
        findViewById(R.id.iv_voice).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVoiceMode();
            }
        });
        ivEmoji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEmojiPanel();
            }
        });
        // 「+」不再只弹提示，改成真正能用的功能面板（红包 / 小游戏 / 相册都是通的）
        ivAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMorePanel();
            }
        });
    }

    /** 语音模式：输入框收起来，换成「按住 说话」按钮 */
    private void toggleVoiceMode() {
        boolean toVoice = tvHoldTalk.getVisibility() != View.VISIBLE;
        tvHoldTalk.setVisibility(toVoice ? View.VISIBLE : View.GONE);
        etInput.setVisibility(toVoice ? View.GONE : View.VISIBLE);
        if (toVoice) {
            tvSend.setVisibility(View.GONE);
        } else {
            // 回到文字模式，按有没有内容恢复「发送」按钮
            boolean hasText = etInput.getText().toString().trim().length() > 0;
            tvSend.setVisibility(hasText ? View.VISIBLE : View.GONE);
        }
        initHoldTalk();
    }

    /** 「按住 说话」：按下开始计时并盖住列表，松开按秒数发一条语音 */
    private void initHoldTalk() {
        tvHoldTalk.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        talkStartAt = System.currentTimeMillis();
                        tvHoldTalk.setText(R.string.voice_release_send);
                        tvTalkOverlay.setText(R.string.voice_release_send);
                        tvTalkOverlay.setVisibility(View.VISIBLE);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        tvTalkOverlay.setVisibility(View.GONE);
                        tvHoldTalk.setText(R.string.voice_hold_talk);
                        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                            int seconds = (int) ((System.currentTimeMillis() - talkStartAt) / 1000);
                            if (seconds < 1) {
                                Toast.makeText(ChatActivity.this, R.string.voice_too_short,
                                        Toast.LENGTH_SHORT).show();
                                return true;
                            }
                            ChatManager.sendVoice(chatId, seconds);
                            refreshMessages();
                            scrollToBottom();
                            scheduleAutoReply("语音");
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    /**
     * 底部「+」面板：相册 / 拍摄 / 视频通话 / 位置 / 小游戏。
     * 相册和小游戏是通的，其余保持演示提示。
     */
    private void showMorePanel() {
        final Dialog dialog = new Dialog(this, R.style.BottomSheetDialog);
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_chat_more, null);
        dialog.setContentView(content);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }

        content.findViewById(R.id.item_game).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(ChatActivity.this, GameActivity.class);
                intent.putExtra("chatId", chatId);
                intent.putExtra("chatName", chatName);
                intent.putExtra("chatAvatar", chatAvatar);
                startActivity(intent);
            }
        });

        content.findViewById(R.id.item_redpacket).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showRedPacketSendDialog();
            }
        });

        content.findViewById(R.id.item_transfer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showTransferSendDialog();
            }
        });

        content.findViewById(R.id.item_album).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                pickImage();
            }
        });

        View.OnClickListener notYet = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ChatActivity.this, R.string.game_not_supported,
                        Toast.LENGTH_SHORT).show();
            }
        };
        content.findViewById(R.id.item_camera).setOnClickListener(notYet);
        content.findViewById(R.id.item_video).setOnClickListener(notYet);
        content.findViewById(R.id.item_location).setOnClickListener(notYet);

        dialog.show();
    }

    /** 打开系统相册选一张图 */
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.chat_image_pick)), REQ_PICK_IMAGE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.chat_image_none, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_IMAGE || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        final Uri uri = data.getData();
        // 压缩、转正、落盘都是耗时操作，放后台线程，别卡住输入框
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String path = MomentPhotoStore.importFromUri(ChatActivity.this, uri);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) {
                            if (path != null) {
                                MomentPhotoStore.deleteQuietly(path);
                            }
                            return;
                        }
                        if (path == null) {
                            Toast.makeText(ChatActivity.this, R.string.chat_image_failed,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ChatManager.sendImage(chatId, path);
                        refreshMessages();
                        scrollToBottom();
                        // 本地演示模式下，发图也让对方回一句，不然只有自己一条消息有点冷清
                        scheduleAutoReply(getString(R.string.chat_image_summary));
                    }
                });
            }
        }).start();
    }

    /** 首次进入时 ChatManager 可能还没数据（比如从联系人资料页直接跳过来），先加载一次 */
    private void ensureData() {
        if (ChatManager.isInited()) {
            refreshMessages();
            scrollToBottom();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String json = WeixinDataUtils.loadWeixinJson(ChatActivity.this);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ChatManager.init(WeixinDataUtils.parseChatList(json));
                        refreshMessages();
                        scrollToBottom();
                    }
                });
            }
        }).start();
    }

    /** 发送消息：写进 ChatManager，刷新列表，并安排一条模拟回复 */
    private void sendMessage() {
        String content = etInput.getText().toString().trim();
        if (content.isEmpty()) {
            return;
        }
        ChatManager.send(chatId, content);
        etInput.setText("");
        refreshMessages();
        scrollToBottom();
        scheduleAutoReply(content);
    }

    /**
     * 安排一条"对方回复"。
     * 先看这句话有没有命中关键词规则（你好 / 吃饭了吗 / 你喜欢谁），
     * 命中了就回固定内容，没命中才用原来的随机回复。
     */
    private void scheduleAutoReply(String input) {
        // 真连上服务器时，对面是活人在打字，就不要再伪造回复；
        // 但只是勾了"在线模式"却没连上（比如自己一个人演示），仍然让关键词回复生效
        if ((AppPrefs.isLanMode() && ChatClient.get().isConnected())
                || Constant.AUTO_REPLY_DELAY <= 0) {
            return;
        }
        // 连续发多条时取消上一条，只回最后一句
        if (input != null && input.contains("红包")) {
            // 跟对方提「红包」，对方就真的发一个过来
            pendingFriendRedPacket = true;
            pendingReply = null;
        } else {
            pendingReply = AutoReplyRules.replyFor(input);
        }
        handler.removeCallbacks(autoReplyTask);
        handler.postDelayed(autoReplyTask, Constant.AUTO_REPLY_DELAY);
    }

    // ---------------- 红包 ----------------

    /** 发红包弹窗：填金额 + 留言 */
    private void showRedPacketSendDialog() {
        final Dialog dialog = new Dialog(this, R.style.BottomSheetDialog);
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_redpacket_send, null);
        dialog.setContentView(content);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }

        final EditText etAmount = content.findViewById(R.id.et_rp_amount);
        final EditText etGreeting = content.findViewById(R.id.et_rp_greeting);
        etGreeting.setText(R.string.rp_greeting_hint);

        content.findViewById(R.id.btn_rp_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amount = etAmount.getText().toString().trim();
                double value;
                try {
                    value = Double.parseDouble(amount);
                } catch (NumberFormatException e) {
                    value = 0;
                }
                if (value <= 0 || value > 888) {
                    Toast.makeText(ChatActivity.this, R.string.rp_amount_invalid,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                MsgBean msg = ChatManager.sendRedPacket(chatId,
                        etGreeting.getText().toString().trim(),
                        String.format(java.util.Locale.CHINA, "%.2f", value));
                dialog.dismiss();
                refreshMessages();
                scrollToBottom();
                Toast.makeText(ChatActivity.this, R.string.rp_sent, Toast.LENGTH_SHORT).show();

                // 演示模式：过一会儿对方拆开红包并道谢
                if (msg != null) {
                    scheduleFriendOpen(msg);
                }
            }
        });

        dialog.show();
    }

    /** 对方拆我发的红包：更新领取状态 + 回一句道谢 */
    private void scheduleFriendOpen(final MsgBean msg) {
        if (AppPrefs.isLanMode() && ChatClient.get().isConnected()) {
            return; // 真人在线时不要伪造
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                ChatManager.openRedPacket(chatId, msg);
                ChatManager.receive(chatId, getString(R.string.rp_friend_thanks));
                refreshMessages();
            }
        }, Constant.AUTO_REPLY_DELAY);
    }

    /** 拆红包弹窗：金色「開」→ 显示金额；已拆过的直接显示结果 */
    private void showRedPacketOpenDialog(final MsgBean msg) {
        if (msg.redPacketOpened()) {
            showRedPacketResult(msg);
            return;
        }
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_redpacket_open);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        String displayName = msg.isFromMe()
                ? getString(R.string.rp_from_me)
                : getString(R.string.rp_from_other, RemarkDao.displayName(chatName));
        ((TextView) dialog.findViewById(R.id.tv_rp_from)).setText(displayName);
        ((TextView) dialog.findViewById(R.id.tv_rp_greeting_big))
                .setText(msg.redPacketGreeting());

        dialog.findViewById(R.id.btn_rp_open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatManager.openRedPacket(chatId, msg);
                showResultInside(dialog, msg);
                refreshMessages();
                if (!msg.isFromMe()) {
                    // 拆了对方的红包，客气一下
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing() && !isDestroyed()) {
                                ChatManager.receive(chatId,
                                        "收到你的红包啦，谢谢！\n也祝你大吉大利~");
                                refreshMessages();
                            }
                        }
                    }, Constant.AUTO_REPLY_DELAY);
                }
            }
        });

        dialog.findViewById(R.id.btn_rp_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /** 「開」按下后的动画：按钮隐去，金额浮现 */
    private void showResultInside(final Dialog dialog, MsgBean msg) {
        TextView btnOpen = dialog.findViewById(R.id.btn_rp_open);
        TextView tvResult = dialog.findViewById(R.id.tv_rp_result);
        TextView tvTip = dialog.findViewById(R.id.tv_rp_tip);
        btnOpen.setVisibility(View.GONE);
        tvTip.setVisibility(View.GONE);
        tvResult.setVisibility(View.VISIBLE);
        tvResult.setText(getString(R.string.rp_opened_by_me, msg.redPacketAmount()));
        tvResult.setAlpha(0f);
        tvResult.animate().alpha(1f).setDuration(400).start();
    }

    /** 已领过的红包再点开：直接看结果 */
    private void showRedPacketResult(final MsgBean msg) {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_redpacket_open);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        String displayName = msg.isFromMe()
                ? getString(R.string.rp_from_me)
                : getString(R.string.rp_from_other, RemarkDao.displayName(chatName));
        ((TextView) dialog.findViewById(R.id.tv_rp_from)).setText(displayName);
        ((TextView) dialog.findViewById(R.id.tv_rp_greeting_big)).setText(msg.redPacketGreeting());
        dialog.findViewById(R.id.btn_rp_open).setVisibility(View.GONE);
        dialog.findViewById(R.id.tv_rp_tip).setVisibility(View.GONE);
        TextView tvResult = dialog.findViewById(R.id.tv_rp_result);
        tvResult.setVisibility(View.VISIBLE);
        tvResult.setText(getString(R.string.rp_opened_by_me, msg.redPacketAmount()));

        dialog.findViewById(R.id.btn_rp_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    // ---------------- 转账 ----------------

    /** 转账弹窗：填金额 + 转账说明 */
    private void showTransferSendDialog() {
        final Dialog dialog = new Dialog(this, R.style.BottomSheetDialog);
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_transfer_send, null);
        dialog.setContentView(content);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }

        final EditText etAmount = content.findViewById(R.id.et_tf_amount);
        final EditText etNote = content.findViewById(R.id.et_tf_note);

        content.findViewById(R.id.btn_tf_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amount = etAmount.getText().toString().trim();
                double value;
                try {
                    value = Double.parseDouble(amount);
                } catch (NumberFormatException e) {
                    value = 0;
                }
                if (value <= 0 || value > 50000) {
                    Toast.makeText(ChatActivity.this, R.string.rp_amount_invalid,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                MsgBean msg = ChatManager.sendTransfer(chatId,
                        etNote.getText().toString().trim(),
                        String.format(java.util.Locale.CHINA, "%.2f", value));
                dialog.dismiss();
                refreshMessages();
                scrollToBottom();
                Toast.makeText(ChatActivity.this, R.string.transfer_sent,
                        Toast.LENGTH_SHORT).show();

                // 演示模式：过一会儿对方确认收款并道谢
                if (msg != null) {
                    scheduleFriendAccept(msg);
                }
            }
        });

        dialog.show();
    }

    /** 对方收我发的转账：更新卡片状态 + 回一句道谢 */
    private void scheduleFriendAccept(final MsgBean msg) {
        if (AppPrefs.isLanMode() && ChatClient.get().isConnected()) {
            return; // 真人在线时不要伪造
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                ChatManager.acceptTransfer(chatId, msg);
                ChatManager.receivePat(chatId,
                        getString(R.string.transfer_friend_accepted));
                ChatManager.receive(chatId, getString(R.string.transfer_friend_thanks));
                refreshMessages();
                scrollToBottom();
            }
        }, Constant.AUTO_REPLY_DELAY);
    }

    /** 点转账卡片：对方转来且未收款的，弹确认收款；其余只提示状态 */
    private void onTransferCardClick(final MsgBean msg) {
        if (msg.isFromMe() || msg.transferReceived()) {
            Toast.makeText(this,
                    msg.transferReceived() ? R.string.transfer_state_done
                            : R.string.transfer_state_wait,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.transfer_receive_title)
                .setMessage(getString(R.string.transfer_receive_msg,
                        RemarkDao.displayName(chatName), msg.transferAmount()))
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.transfer_receive_ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ChatManager.acceptTransfer(chatId, msg);
                                ChatManager.sendPat(chatId,
                                        getString(R.string.transfer_received_tip,
                                                msg.transferAmount()));
                                refreshMessages();
                                scrollToBottom();
                            }
                        })
                .show();
    }

    // ---------------- 拍一拍 ----------------

    /** 双击头像触发拍一拍：先出自己这行提示，演示模式下对方过会儿拍回来 */
    private void doPat(boolean patSelf) {
        String displayName = RemarkDao.displayName(chatName);
        ChatManager.sendPat(chatId, patSelf
                ? getString(R.string.pat_you_pat_self)
                : getString(R.string.pat_you_pat, displayName));
        refreshMessages();
        scrollToBottom();
        // 演示模式：对方也拍回我一下
        if (!(AppPrefs.isLanMode() && ChatClient.get().isConnected())
                && Constant.AUTO_REPLY_DELAY > 0) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    ChatManager.receivePat(chatId,
                            getString(R.string.pat_other_pat,
                                    RemarkDao.displayName(chatName)));
                    refreshMessages();
                    scrollToBottom();
                }
            }, Constant.AUTO_REPLY_DELAY);
        }
    }

    // ---------------- 表情面板 ----------------

    /** 微信经典小黄脸 + 常用符号（Unicode 表情，系统字体直接渲染） */
    private static final String[] EMOJIS = {
            "😀", "😁", "😂", "🤣", "😃", "😄", "😅", "😆",
            "😉", "😊", "😋", "😎", "😍", "😘", "🥰", "😗",
            "🙂", "🤗", "🤔", "😐", "😑", "😶", "🙄", "😏",
            "😣", "😥", "😮", "🤐", "😯", "😪", "😫", "😴",
            "😌", "😛", "😜", "😝", "🤤", "😒", "😓", "😔",
            "😕", "🙃", "🤑", "😲", "🙁", "😖", "😞", "😟",
            "😤", "😢", "😭", "😦", "😧", "😨", "😩", "🤯",
            "😬", "😰", "😱", "🥵", "🥶", "😳", "🤪", "😵",
            "😡", "😠", "🤬", "😷", "🤒", "🤕", "🤢", "🤮",
            "🥴", "😇", "🥳", "🥺", "🤠", "🤡", "🤥", "🤫",
            "👍", "👎", "👏", "🙏", "💪", "🤝", "✌", "👌",
            "❤", "💔", "🎉", "🎂", "🌹", "🍉", "🍺", "🧧"
    };

    /** 点表情按钮：底部弹出表情面板，点一个就插进输入框 */
    private void showEmojiPanel() {
        final Dialog dialog = new Dialog(this, R.style.BottomSheetDialog);
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_emoji_panel, null);
        dialog.setContentView(content);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }

        List<Map<String, String>> data = new ArrayList<>();
        for (String emoji : EMOJIS) {
            Map<String, String> item = new HashMap<>();
            item.put("emoji", emoji);
            data.add(item);
        }
        GridView grid = content.findViewById(R.id.grid_emoji);
        SimpleAdapter emojiAdapter = new SimpleAdapter(this, data,
                R.layout.item_emoji, new String[]{"emoji"}, new int[]{R.id.tv_emoji});
        grid.setAdapter(emojiAdapter);
        grid.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view,
                                    int position, long id) {
                // 插到光标位置；没有焦点就追加到末尾
                int start = Math.max(etInput.getSelectionStart(), 0);
                etInput.getText().insert(start, EMOJIS[position]);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    // ---------------- 消息长按菜单 ----------------

    /** 长按气泡：复制 / 收藏 / 撤回（自己的、2 分钟内）/ 删除 */
    private void showMsgMenu(final MsgBean msg) {
        List<String> items = new ArrayList<>();
        final List<Integer> actions = new ArrayList<>();
        final int ACT_COPY = 0;
        final int ACT_RECALL = 1;
        final int ACT_DELETE = 2;
        final int ACT_FAVORITE = 3;

        if (!msg.isRedPacket() && !msg.isTransfer() && !msg.isVoice() && !msg.isImage()
                && msg.getContent() != null && msg.getContent().length() > 0) {
            items.add(getString(R.string.msg_menu_copy));
            actions.add(ACT_COPY);
        }
        // 除拍一拍系统提示外都能收藏（文字/图片/红包/语音/转账）
        if (!msg.isPat()) {
            items.add(getString(R.string.msg_menu_favorite));
            actions.add(ACT_FAVORITE);
        }
        if (msg.canRecall()) {
            items.add(getString(R.string.msg_menu_recall));
            actions.add(ACT_RECALL);
        }
        items.add(getString(R.string.msg_menu_delete));
        actions.add(ACT_DELETE);

        new AlertDialog.Builder(this)
                .setItems(items.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int action = actions.get(which);
                        if (action == ACT_COPY) {
                            ClipboardManager cm = (ClipboardManager)
                                    getSystemService(CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(ClipData.newPlainText("msg", msg.getContent()));
                            Toast.makeText(ChatActivity.this, R.string.msg_copied,
                                    Toast.LENGTH_SHORT).show();
                        } else if (action == ACT_FAVORITE) {
                            favoriteMsg(msg);
                        } else if (action == ACT_RECALL) {
                            ChatManager.recall(chatId, msg);
                            refreshMessages();
                        } else {
                            ChatManager.deleteMsg(chatId, msg);
                            refreshMessages();
                            Toast.makeText(ChatActivity.this, R.string.msg_deleted,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    /** 把一条消息存进收藏（"我-收藏"里能看） */
    private void favoriteMsg(MsgBean msg) {
        FavoriteBean fav = new FavoriteBean();
        fav.setChatId(chatId);
        fav.setFromName(RemarkDao.displayName(chatName));
        fav.setFromMe(msg.isFromMe());
        fav.setType(msg.getType());
        fav.setContent(msg.getContent());
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm", java.util.Locale.CHINA);
        fav.setTime(fmt.format(new java.util.Date()));
        fav.setTs(System.currentTimeMillis());
        FavoriteDao.insert(fav);
        Toast.makeText(this, R.string.msg_favorited, Toast.LENGTH_SHORT).show();
    }

    private void refreshMessages() {
        List<MsgBean> messages = ChatManager.getMessages(chatId);
        adapter.setData(messages);
    }

    private void scrollToBottom() {
        listMsg.post(new Runnable() {
            @Override
            public void run() {
                if (adapter.getCount() > 0) {
                    listMsg.setSelection(adapter.getCount() - 1);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        // 自动回复、对方拆红包这类延迟任务全部取消，防止退出后还改数据
        handler.removeCallbacksAndMessages(null);
        ChatManager.removeListener(chatListener);
        super.onDestroy();
    }
}
