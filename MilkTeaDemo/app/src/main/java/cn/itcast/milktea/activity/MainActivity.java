package cn.itcast.milktea.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import cn.itcast.milktea.R;
import cn.itcast.milktea.fragment.HomeFragment;
import cn.itcast.milktea.fragment.MineFragment;
import cn.itcast.milktea.fragment.OrdersFragment;

/**
 * 主界面：底部三个 Tab（点单 / 订单 / 我的）。
 *
 * Fragment 用 add + show/hide 管理，而不是 replace：
 * replace 会销毁上一个 Fragment 的 View，来回切 Tab 时列表滚动位置、输入内容都会丢；
 * add + show/hide 只是把 View 藏起来，状态能保留，切换也更快。
 */
public class MainActivity extends AppCompatActivity {

    public static final int TAB_HOME = 0;
    public static final int TAB_ORDERS = 1;
    public static final int TAB_MINE = 2;

    private HomeFragment homeFragment;
    private OrdersFragment ordersFragment;
    private MineFragment mineFragment;

    private Fragment currentFragment;
    private int currentTab = TAB_HOME;

    private TextView[] navTexts;
    private ImageView[] navIcons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navTexts = new TextView[]{
                findViewById(R.id.tv_nav_home),
                findViewById(R.id.tv_nav_orders),
                findViewById(R.id.tv_nav_mine)};
        navIcons = new ImageView[]{
                findViewById(R.id.iv_nav_home),
                findViewById(R.id.iv_nav_orders),
                findViewById(R.id.iv_nav_mine)};

        initFragments();
        initNav();
        // 支持外部指定打开哪个 Tab（比如收银台返回时直接落到"订单"）
        selectTab(getIntent().getIntExtra("tab", TAB_HOME));
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        selectTab(intent.getIntExtra("tab", currentTab));
    }

    /** 三个 Fragment 只创建一次，之后只做 show/hide */
    private void initFragments() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();

        homeFragment = new HomeFragment();
        ordersFragment = new OrdersFragment();
        mineFragment = new MineFragment();

        tx.add(R.id.fl_container, homeFragment, "home");
        tx.add(R.id.fl_container, ordersFragment, "orders");
        tx.add(R.id.fl_container, mineFragment, "mine");
        tx.hide(ordersFragment);
        tx.hide(mineFragment);
        tx.commit();
        currentFragment = homeFragment;
    }

    private void initNav() {
        findViewById(R.id.ll_nav_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(TAB_HOME);
            }
        });
        findViewById(R.id.ll_nav_orders).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(TAB_ORDERS);
            }
        });
        findViewById(R.id.ll_nav_mine).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(TAB_MINE);
            }
        });
    }

    /**
     * 切换 Tab。public 是给 Fragment 用的，比如"去点单"按钮要跳回首页。
     */
    public void selectTab(int tab) {
        Fragment target;
        if (tab == TAB_ORDERS) {
            target = ordersFragment;
        } else if (tab == TAB_MINE) {
            target = mineFragment;
        } else {
            target = homeFragment;
        }

        if (target != currentFragment) {
            FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
            tx.hide(currentFragment);
            tx.show(target);
            tx.commit();
            currentFragment = target;
        }
        currentTab = tab;
        refreshNavState();
    }

    /** 选中的 Tab 用主色 + 不透明图标，未选中用灰色 + 半透明 */
    private void refreshNavState() {
        int selectedColor = getResources().getColor(R.color.primary);
        int normalColor = getResources().getColor(R.color.text_gray);
        for (int i = 0; i < navTexts.length; i++) {
            boolean selected = i == currentTab;
            navTexts[i].setTextColor(selected ? selectedColor : normalColor);
            navTexts[i].setTypeface(null, selected
                    ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            navIcons[i].setImageAlpha(selected ? 255 : 110);
        }
    }

    /** 从任意页面回到首页（"继续点单"） */
    public void backToHome() {
        selectTab(TAB_HOME);
    }
}
