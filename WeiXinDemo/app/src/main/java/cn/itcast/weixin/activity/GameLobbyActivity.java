package cn.itcast.weixin.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.weixin.R;
import cn.itcast.weixin.bean.ChatBean;
import cn.itcast.weixin.bean.ContactBean;
import cn.itcast.weixin.utils.ChatManager;
import cn.itcast.weixin.utils.GameStats;
import cn.itcast.weixin.utils.WeixinDataUtils;

/**
 * 游戏大厅（发现 → 游戏）。
 *
 * 这里做三件事：展示累计战绩、列出三个玩法、让用户挑一个联系人。
 * 真正的玩法在 GameActivity，那边也支持从聊天页「+」直接带着对手进来。
 */
public class GameLobbyActivity extends AppCompatActivity {

    private final List<ContactBean> contacts = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvStatRps;
    private TextView tvStatGuess;
    private TextView tvStatMemory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_lobby);

        ((TextView) findViewById(R.id.tv_title)).setText(R.string.game_hall_title);
        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.iv_right).setVisibility(View.GONE);

        tvStatRps = findViewById(R.id.tv_stat_rps);
        tvStatGuess = findViewById(R.id.tv_stat_guess);
        tvStatMemory = findViewById(R.id.tv_stat_memory);
        findViewById(R.id.tv_clear_stats).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmClearStats();
            }
        });

        findViewById(R.id.card_rps).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickOpponent(0);
            }
        });
        findViewById(R.id.card_guess).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickOpponent(1);
            }
        });
        findViewById(R.id.card_memory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickOpponent(2);
            }
        });

        loadContacts();
    }

    /** 从对局回来时战绩可能变了，每次回到前台都刷一遍 */
    @Override
    protected void onResume() {
        super.onResume();
        refreshStats();
    }

    private void refreshStats() {
        int win = GameStats.rpsWin();
        int lose = GameStats.rpsLose();
        if (win + lose + GameStats.rpsDraw() > 0) {
            tvStatRps.setText(getString(R.string.game_stats_rps_value, win, lose));
        } else {
            tvStatRps.setText(R.string.game_stats_empty_value);
        }

        int guessBest = GameStats.guessBest();
        tvStatGuess.setText(guessBest > 0
                ? getString(R.string.game_stats_guess_value, guessBest)
                : getString(R.string.game_stats_empty_value));

        int memoryBest = GameStats.memoryBest();
        tvStatMemory.setText(memoryBest > 0
                ? getString(R.string.game_stats_memory_value, memoryBest)
                : getString(R.string.game_stats_empty_value));
    }

    private void confirmClearStats() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.game_stats_clear_title)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.game_stats_clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GameStats.clear();
                        refreshStats();
                        Toast.makeText(GameLobbyActivity.this, R.string.game_stats_cleared,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    /** 联系人只有几个，读一次 assets 就够，放后台线程避免主线程 IO */
    private void loadContacts() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<ContactBean> list = WeixinDataUtils.parseContactList(
                        WeixinDataUtils.loadWeixinJson(GameLobbyActivity.this));
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        contacts.clear();
                        contacts.addAll(list);
                    }
                });
            }
        }).start();
    }

    private void pickOpponent(final int gameIndex) {
        if (contacts.isEmpty()) {
            Toast.makeText(this, R.string.game_no_contact, Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] names = new String[contacts.size()];
        for (int i = 0; i < contacts.size(); i++) {
            names[i] = contacts.get(i).getName();
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.game_pick_opponent)
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ContactBean contact = contacts.get(which);
                        startGame(contact.getName(), contact.getAvatar(), gameIndex);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void startGame(String name, String avatar, int gameIndex) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("chatName", name);
        intent.putExtra("chatAvatar", avatar);
        intent.putExtra("gameIndex", gameIndex);
        // 已经有这个人的会话就带上 chatId，结束后可以直接把战绩发过去
        ChatBean chat = ChatManager.findChatByName(name);
        if (chat != null) {
            intent.putExtra("chatId", chat.getId());
            intent.putExtra("chatAvatar", chat.getAvatar());
        }
        startActivity(intent);
    }
}
