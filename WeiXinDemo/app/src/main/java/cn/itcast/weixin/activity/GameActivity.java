package cn.itcast.weixin.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import cn.itcast.weixin.R;
import cn.itcast.weixin.bean.ChatBean;
import cn.itcast.weixin.db.AvatarDao;
import cn.itcast.weixin.db.RemarkDao;
import cn.itcast.weixin.utils.AvatarStore;
import cn.itcast.weixin.utils.ChatManager;
import cn.itcast.weixin.utils.GameStats;
import cn.itcast.weixin.utils.WeixinDataUtils;
import cn.itcast.weixin.view.CircleImageView;

/**
 * 和联系人玩的小游戏，三个玩法共用一页：
 *
 * 1. 猜拳：石头 / 剪刀 / 布，对手随机出，先做几帧「抖动」再定格，比直接出结果有意思；
 * 2. 猜数字：1~100，七次机会，每次提示大了还是小了；
 * 3. 翻牌配对：4×2 共 8 张牌，翻开两张相同即配对。
 *
 * 三个玩法共用一个比分（我 vs 对手），结束后可以把战绩作为一条聊天消息发出去。
 * 对手就是通讯录里的某个联系人，头像和备注都走全局统一的出口。
 */
public class GameActivity extends AppCompatActivity {

    private static final String[] GESTURES = {"✊", "✌️", "✋"};
    private static final int[] GESTURE_NAMES = {
            R.string.game_rock, R.string.game_scissors, R.string.game_paper
    };
    /** 翻牌用的四组图案，两两成对 */
    private static final String[] CARD_FACES = {"🍎", "🍌", "🍇", "🍉"};

    private int chatId = -1;
    private String opponentName = "";
    private String opponentAvatar = "";

    private int myScore;
    private int opponentScore;
    private int currentGame; // 0 猜拳 1 猜数字 2 翻牌

    private TextView tvOpponent;
    private TextView tvScore;
    private TextView tvTabRps;
    private TextView tvTabGuess;
    private TextView tvTabMemory;
    private LinearLayout llRps;
    private LinearLayout llGuess;
    private LinearLayout llMemory;

    // 猜拳
    private TextView tvRpsOpponent;
    private TextView tvRpsResult;
    private boolean rpsAnimating;

    // 猜数字
    private TextView tvGuessTip;
    private TextView tvGuessHistory;
    private EditText etGuess;
    private TextView btnGuess;
    private int answer;
    private int chancesLeft;
    private boolean guessOver;
    /** 根据每次「大了 / 小了」收窄出来的可能区间，提示玩家现在该往哪儿猜 */
    private int guessLow = 1;
    private int guessHigh = 100;
    private final StringBuilder guessLog = new StringBuilder();

    // 翻牌
    private GridLayout glCards;
    private TextView tvMemorySteps;
    private TextView[] cards = new TextView[8];
    private int[] cardValues = new int[8];
    private boolean[] cardOpen = new boolean[8];
    private boolean[] cardMatched = new boolean[8];
    private int firstOpen = -1;
    private int steps;
    private boolean memoryBusy;
    private boolean memoryDone;
    /** 本局开始时间，用来算通关用时 */
    private long memoryStart;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        chatId = getIntent().getIntExtra("chatId", -1);
        opponentName = safe(getIntent().getStringExtra("chatName"));
        opponentAvatar = safe(getIntent().getStringExtra("chatAvatar"));
        if (opponentName.isEmpty()) {
            opponentName = "好友";
        }
        // 从游戏大厅进来时只有名字，这里补一次会话查找，能发战绩就发
        if (chatId < 0) {
            ChatBean chat = ChatManager.findChatByName(opponentName);
            if (chat != null) {
                chatId = chat.getId();
            } else if (!ChatManager.isInited()) {
                // 冷启动直接进「发现 → 游戏」时会话还没加载过，补一次
                loadChatsThenMatch();
            }
        }

        initTitleBar();
        bindOpponent();
        initTabs();
        initRps();
        initGuess();
        initMemory();
        // 从游戏大厅进来时直接切到对应玩法，从聊天页进来默认猜拳
        selectTab(getIntent().getIntExtra("gameIndex", 0));

        findViewById(R.id.btn_send_result).setOnClickListener(v -> sendResult());
    }

    /** 会话数据还没加载时补一次，加载完再按昵称找到对应的会话 */
    private void loadChatsThenMatch() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<ChatBean> chats = WeixinDataUtils.parseChatList(
                        WeixinDataUtils.loadWeixinJson(GameActivity.this));
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ChatManager.init(chats);
                        ChatBean chat = ChatManager.findChatByName(opponentName);
                        if (chat != null) {
                            chatId = chat.getId();
                        }
                    }
                });
            }
        }).start();
    }

    private void initTitleBar() {        ((TextView) findViewById(R.id.tv_title)).setText(R.string.game_title);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        // 游戏页右上角不需要「更多」
        findViewById(R.id.iv_right).setVisibility(View.GONE);
    }

    /** 顶部对手卡片：头像 + 昵称 + 比分 */
    private void bindOpponent() {
        tvOpponent = findViewById(R.id.tv_opponent);
        tvScore = findViewById(R.id.tv_score);
        CircleImageView iv = findViewById(R.id.iv_opponent);

        String showName = RemarkDao.displayName(opponentName);
        tvOpponent.setText(getString(R.string.game_with, showName));
        updateScore();

        String avatar = AvatarDao.displayAvatar(opponentName, opponentAvatar);
        int fallback = WeixinDataUtils.getDrawableId(this, avatar);
        AvatarStore.apply(iv, avatar, fallback == 0 ? R.drawable.avatar_default : fallback);
    }

    private void updateScore() {
        tvScore.setText(getString(R.string.game_score,
                myScore, opponentScore, RemarkDao.displayName(opponentName)));
    }

    // ---------------- 游戏切换 ----------------

    private void initTabs() {
        tvTabRps = findViewById(R.id.tv_tab_rps);
        tvTabGuess = findViewById(R.id.tv_tab_guess);
        tvTabMemory = findViewById(R.id.tv_tab_memory);
        llRps = findViewById(R.id.ll_rps);
        llGuess = findViewById(R.id.ll_guess);
        llMemory = findViewById(R.id.ll_memory);

        tvTabRps.setOnClickListener(v -> selectTab(0));
        tvTabGuess.setOnClickListener(v -> selectTab(1));
        tvTabMemory.setOnClickListener(v -> selectTab(2));
    }

    private void selectTab(int index) {
        currentGame = index;
        tvTabRps.setSelected(index == 0);
        tvTabGuess.setSelected(index == 1);
        tvTabMemory.setSelected(index == 2);
        llRps.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        llGuess.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        llMemory.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        if (index == 1 && answer == 0) {
            startGuess();
        }
        if (index == 2 && cards[0] == null) {
            startMemory();
        }
    }

    // ---------------- 玩法一：猜拳 ----------------

    private void initRps() {
        tvRpsOpponent = findViewById(R.id.tv_rps_opponent);
        tvRpsResult = findViewById(R.id.tv_rps_result);

        findViewById(R.id.btn_rock).setOnClickListener(v -> playRps(0));
        findViewById(R.id.btn_scissors).setOnClickListener(v -> playRps(1));
        findViewById(R.id.btn_paper).setOnClickListener(v -> playRps(2));
        findViewById(R.id.btn_rps_reset).setOnClickListener(v -> {
            myScore = 0;
            opponentScore = 0;
            updateScore();
            tvRpsResult.setText("");
            tvRpsOpponent.setText("✊");
        });
    }

    private void playRps(final int mine) {
        if (rpsAnimating) {
            return;
        }
        rpsAnimating = true;
        final int theirs = random.nextInt(3);

        // 先让对手的手势「抖」几帧，再定格，比秒出结果有代入感
        int frames = 7;
        for (int i = 0; i < frames; i++) {
            handler.postDelayed(() -> tvRpsOpponent.setText(
                    GESTURES[random.nextInt(GESTURES.length)]), i * 70L);
        }
        handler.postDelayed(() -> {
            tvRpsOpponent.setText(GESTURES[theirs]);
            int diff = (mine - theirs + 3) % 3;
            String mineName = getString(GESTURE_NAMES[mine]);
            String theirName = getString(GESTURE_NAMES[theirs]);
            if (diff == 0) {
                tvRpsResult.setText(getString(R.string.game_draw) + "，" + mineName + " 对 " + theirName);
            } else if (diff == 1) {
                myScore++;
                tvRpsResult.setText(getString(R.string.game_you_win) + "，" + mineName + " 胜 " + theirName);
            } else {
                opponentScore++;
                tvRpsResult.setText(getString(R.string.game_you_lose) + "，" + theirName + " 胜 " + mineName);
            }
            // 每局都记进总战绩，退出 App 也还在
            GameStats.addRps(diff == 0 ? 0 : (diff == 1 ? 1 : -1));
            updateScore();
            rpsAnimating = false;
        }, frames * 70L + 60);
    }

    // ---------------- 玩法二：猜数字 ----------------

    private void initGuess() {
        tvGuessTip = findViewById(R.id.tv_guess_tip);
        tvGuessHistory = findViewById(R.id.tv_guess_history);
        etGuess = findViewById(R.id.et_guess);
        btnGuess = findViewById(R.id.btn_guess);

        btnGuess.setOnClickListener(v -> submitGuess());
        findViewById(R.id.btn_guess_restart).setOnClickListener(v -> startGuess());
    }

    private void startGuess() {
        answer = 1 + random.nextInt(100);
        chancesLeft = 7;
        guessOver = false;
        guessLow = 1;
        guessHigh = 100;
        guessLog.setLength(0);
        guessLog.append(getString(R.string.game_guess_tip)).append("\n");
        tvGuessHistory.setText(guessLog.toString());
        updateGuessTip();
        etGuess.setText("");
        etGuess.setEnabled(true);
        btnGuess.setEnabled(true);
    }

    /** 提示行显示「还能猜的区间 + 剩余次数」，比只说剩几次更有用 */
    private void updateGuessTip() {
        tvGuessTip.setText(getString(R.string.game_guess_range, guessLow, guessHigh, chancesLeft));
    }

    private void submitGuess() {
        if (guessOver) {
            Toast.makeText(this, R.string.game_restart, Toast.LENGTH_SHORT).show();
            return;
        }
        String raw = etGuess.getText().toString().trim();
        if (raw.isEmpty()) {
            Toast.makeText(this, R.string.game_guess_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        int value;
        try {
            value = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return;
        }
        if (value < 1 || value > 100) {
            Toast.makeText(this, "请输入 1~100", Toast.LENGTH_SHORT).show();
            return;
        }

        chancesLeft--;
        int used = 7 - chancesLeft;
        etGuess.setText("");

        if (value == answer) {
            guessOver = true;
            myScore++;
            updateScore();
            tvGuessTip.setText(getString(R.string.game_guess_right)
                    + " " + getString(R.string.game_guess_used, used));
            appendGuess("第 " + used + " 次：" + value + " ✓ 猜中");
            GameStats.addGuessWin(used);
            endGuess();
            return;
        }

        // 猜偏了就收窄区间：比答案大，答案最多到 value-1；反之从 value+1 起
        if (value > answer) {
            guessHigh = Math.min(guessHigh, value - 1);
        } else {
            guessLow = Math.max(guessLow, value + 1);
        }

        String hint = value > answer
                ? getString(R.string.game_guess_big)
                : getString(R.string.game_guess_small);
        appendGuess("第 " + used + " 次：" + value + " → " + hint);

        if (chancesLeft <= 0) {
            guessOver = true;
            opponentScore++;
            updateScore();
            tvGuessTip.setText(getString(R.string.game_guess_over, answer));
            endGuess();
        } else {
            updateGuessTip();
        }
    }

    private void appendGuess(String line) {
        guessLog.append(line).append("\n");
        tvGuessHistory.setText(guessLog.toString());
    }

    private void endGuess() {
        etGuess.setEnabled(false);
        btnGuess.setEnabled(false);
    }

    // ---------------- 玩法三：翻牌配对 ----------------

    private void initMemory() {
        glCards = findViewById(R.id.gl_cards);
        tvMemorySteps = findViewById(R.id.tv_memory_steps);
        findViewById(R.id.btn_memory_restart).setOnClickListener(v -> startMemory());
    }

    private void startMemory() {
        // 四组图案各两张，打乱后铺进 4×2 的网格
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < CARD_FACES.length; i++) {
            values.add(i);
            values.add(i);
        }
        Collections.shuffle(values);
        for (int i = 0; i < 8; i++) {
            cardValues[i] = values.get(i);
            cardOpen[i] = false;
            cardMatched[i] = false;
        }
        firstOpen = -1;
        steps = 0;
        memoryBusy = false;
        memoryDone = false;
        memoryStart = System.currentTimeMillis();
        tvMemorySteps.setText(getString(R.string.game_memory_progress, 0, 0));

        // 每秒刷一次用时，让「翻牌」不只是一个静态的步数计数
        handler.removeCallbacks(memoryTicker);
        handler.postDelayed(memoryTicker, 1000);

        glCards.removeAllViews();
        cards = new TextView[8];
        int size = (getResources().getDisplayMetrics().widthPixels - dp(28 + 32 + 40)) / 4;
        for (int i = 0; i < 8; i++) {
            final int index = i;
            TextView card = new TextView(this);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = size;
            lp.height = dp(62);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            card.setLayoutParams(lp);
            card.setGravity(Gravity.CENTER);
            card.setTextSize(26);
            card.setBackgroundResource(R.drawable.bg_card_back);
            card.setText("?");
            card.setTextColor(getResources().getColor(R.color.weixin_green));
            card.setOnClickListener(v -> flipCard(index));
            glCards.addView(card);
            cards[i] = card;
        }
    }

    private void flipCard(int index) {
        if (memoryBusy || memoryDone || cardOpen[index] || cardMatched[index]) {
            return;
        }
        openCard(index, true);

        if (firstOpen == -1) {
            firstOpen = index;
            return;
        }

        final int a = firstOpen;
        final int b = index;
        firstOpen = -1;
        steps++;
        updateMemoryProgress();

        if (cardValues[a] == cardValues[b]) {
            cardMatched[a] = true;
            cardMatched[b] = true;
            cards[a].setBackgroundResource(R.drawable.bg_card_matched);
            cards[b].setBackgroundResource(R.drawable.bg_card_matched);
            if (allMatched()) {
                memoryDone = true;
                handler.removeCallbacks(memoryTicker);
                myScore++;
                updateScore();
                int seconds = memorySeconds();
                tvMemorySteps.setText(getString(R.string.game_memory_done2, steps, seconds));
                if (GameStats.addMemoryWin(steps)) {
                    Toast.makeText(this, getString(R.string.game_memory_record, steps),
                            Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }

        // 不一样：停留 650ms 让玩家看清，再翻回去
        memoryBusy = true;
        handler.postDelayed(() -> {
            openCard(a, false);
            openCard(b, false);
            memoryBusy = false;
        }, 650);
    }

    private void openCard(int index, boolean open) {
        cardOpen[index] = open;
        cards[index].setText(open ? CARD_FACES[cardValues[index]] : "?");
        cards[index].setTextColor(getResources().getColor(
                open ? R.color.text_primary : R.color.weixin_green));
    }

    private boolean allMatched() {
        for (int i = 0; i < 8; i++) {
            if (!cardMatched[i]) {
                return false;
            }
        }
        return true;
    }

    /** 本局翻牌已经用了几秒 */
    private int memorySeconds() {
        if (memoryStart == 0) {
            return 0;
        }
        return (int) Math.max(0, (System.currentTimeMillis() - memoryStart) / 1000);
    }

    private void updateMemoryProgress() {
        if (memoryDone) {
            return;
        }
        tvMemorySteps.setText(getString(R.string.game_memory_progress, steps, memorySeconds()));
    }

    /** 每秒跑一次，只为了刷新用时数字；通关或退出时会被移除 */
    private final Runnable memoryTicker = new Runnable() {
        @Override
        public void run() {
            if (memoryDone || isFinishing()) {
                return;
            }
            updateMemoryProgress();
            handler.postDelayed(this, 1000);
        }
    };

    // ---------------- 战绩 ----------------

    private void sendResult() {
        if (chatId < 0) {
            Toast.makeText(this, "先和「" + RemarkDao.displayName(opponentName)
                    + "」聊过天，才能把战绩发过去", Toast.LENGTH_SHORT).show();
            return;
        }
        String gameName;
        if (currentGame == 0) {
            gameName = getString(R.string.game_stone_scissors_paper);
        } else if (currentGame == 1) {
            gameName = getString(R.string.game_guess_number);
        } else {
            gameName = getString(R.string.game_memory);
        }
        String text = "【小游戏·" + gameName + "】我 " + myScore + " : " + opponentScore
                + " " + RemarkDao.displayName(opponentName) + "，再来一局？";
        ChatManager.send(chatId, text);
        Toast.makeText(this, R.string.game_result_sent, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        // 猜拳的抖动、翻牌的延时回翻都要在这里取消，否则页面关了还在改 View
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
