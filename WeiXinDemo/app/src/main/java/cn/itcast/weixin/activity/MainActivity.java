package cn.itcast.weixin.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import cn.itcast.weixin.R;
import cn.itcast.weixin.fragment.ChatsFragment;
import cn.itcast.weixin.fragment.ContactsFragment;
import cn.itcast.weixin.fragment.DiscoverFragment;
import cn.itcast.weixin.fragment.MeFragment;
import cn.itcast.weixin.utils.AppPrefs;
import cn.itcast.weixin.utils.ChatClient;
import cn.itcast.weixin.utils.ChatManager;

/**
 * 主界面：底部四个 Tab（微信 / 通讯录 / 发现 / 我）的容器。
 *
 * 切换用的是 Fragment 的 add + show/hide，而不是 replace：
 * 这样来回切换时页面不会被销毁重建，列表滚动位置、输入状态都能保留。
 *
 * 局域网模式下的连接状态和服务器通知（比如"对方不在线"）统一在这里弹 Toast。
 */
public class MainActivity extends AppCompatActivity {

    /** 微信 Tab 的下标，红点只挂在这一个 Tab 上 */
    private static final int TAB_CHAT = 0;

    private final Fragment[] fragments = new Fragment[4];
    private final View[] tabViews = new View[4];
    private final ImageView[] tabIcons = new ImageView[4];
    private final TextView[] tabNames = new TextView[4];

    private final int[] tabIds = {R.id.tab_chat, R.id.tab_contacts, R.id.tab_discover, R.id.tab_me};
    private final int[] iconNormal = {R.drawable.ic_tab_chat, R.drawable.ic_tab_contacts,
            R.drawable.ic_tab_discover, R.drawable.ic_tab_me};
    private final int[] iconSelected = {R.drawable.ic_tab_chat_sel, R.drawable.ic_tab_contacts_sel,
            R.drawable.ic_tab_discover_sel, R.drawable.ic_tab_me_sel};

    private TextView tvTitle;
    private ImageView ivRight;
    private TextView tvChatBadge;

    /** 当前选中的 Tab 下标，-1 表示还没有选中任何 Tab */
    private int currentTab = -1;

    /** 服务器连接状态变化、以及服务器发来的提示，都用 Toast 告诉用户 */
    private final ChatClient.Listener clientListener = new ChatClient.Listener() {
        @Override
        public void onConnectionChanged(boolean connected, String hint) {
            if (AppPrefs.isLanMode()) {
                Toast.makeText(MainActivity.this, hint, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onNotice(String message) {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTitle = findViewById(R.id.tv_title);
        ivRight = findViewById(R.id.iv_right);
        // 主界面没有上一级，返回按钮藏起来
        findViewById(R.id.iv_back).setVisibility(View.GONE);

        for (int i = 0; i < tabIds.length; i++) {
            View tab = findViewById(tabIds[i]);
            tabViews[i] = tab;
            tabIcons[i] = tab.findViewById(R.id.iv_icon);
            tabNames[i] = tab.findViewById(R.id.tv_name);
            tabNames[i].setText(getString(tabTitleRes(i)));
            final int index = i;
            tab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectTab(index);
                }
            });
        }
        // 微信 Tab 图标右上角的未读总数红点
        tvChatBadge = tabViews[TAB_CHAT].findViewById(R.id.tv_badge);

        ivRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, R.string.more, Toast.LENGTH_SHORT).show();
            }
        });

        selectTab(TAB_CHAT);

        // 局域网模式下开机就按设置去连服务器（连不上会自己重试）
        ChatClient.get().addListener(clientListener);
        ChatClient.get().connectIfNeeded();
    }

    @Override
    protected void onDestroy() {
        ChatClient.get().removeListener(clientListener);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从聊天页回来时同步一下红点（ChatsFragment 也会刷一次，这里是双保险）
        updateChatBadge();
    }

    /** 切换 Tab：第一次进来 add，之后再进来只做 show/hide */
    private void selectTab(int index) {
        if (index == currentTab) {
            return;
        }
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        Fragment target = fragments[index];
        if (target == null) {
            target = createFragment(index);
            fragments[index] = target;
            transaction.add(R.id.fl_container, target, "tab" + index);
        } else {
            transaction.show(target);
        }
        if (currentTab >= 0 && fragments[currentTab] != null) {
            transaction.hide(fragments[currentTab]);
        }
        transaction.commit();

        // 刷新底部四个 Tab 的图标与文字颜色
        for (int i = 0; i < tabViews.length; i++) {
            boolean selected = i == index;
            tabIcons[i].setImageResource(selected ? iconSelected[i] : iconNormal[i]);
            tabNames[i].setTextColor(ContextCompat.getColor(this,
                    selected ? R.color.weixin_green : R.color.icon_gray));
        }

        // 标题栏文字跟着变
        tvTitle.setText(getString(tabTitleRes(index)));

        // 只有“微信”Tab 的右上角有 + 号
        if (index == TAB_CHAT) {
            ivRight.setImageResource(R.drawable.ic_plus);
            ivRight.setVisibility(View.VISIBLE);
        } else {
            ivRight.setVisibility(View.INVISIBLE);
        }

        currentTab = index;
    }

    private Fragment createFragment(int index) {
        switch (index) {
            case 1:
                return new ContactsFragment();
            case 2:
                return new DiscoverFragment();
            case 3:
                return new MeFragment();
            default:
                return new ChatsFragment();
        }
    }

    private int tabTitleRes(int index) {
        switch (index) {
            case 1:
                return R.string.tab_contacts;
            case 2:
                return R.string.tab_discover;
            case 3:
                return R.string.tab_me;
            default:
                return R.string.tab_chat;
        }
    }

    /**
     * 刷新“微信”Tab 上的未读总数红点。
     * ChatsFragment 每次刷新列表后都会回调这里，所以发完消息返回即可看到红点变化。
     */
    public void updateChatBadge() {
        if (tvChatBadge == null) {
            return;
        }
        int total = ChatManager.totalUnread();
        if (total > 0) {
            tvChatBadge.setVisibility(View.VISIBLE);
            tvChatBadge.setText(total > 99 ? "99+" : String.valueOf(total));
        } else {
            tvChatBadge.setVisibility(View.GONE);
        }
    }
}
