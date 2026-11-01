package cn.itcast.order.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import cn.itcast.order.R;
import cn.itcast.order.adapter.OrderCenterAdapter;
import cn.itcast.order.bean.FoodBean;
import cn.itcast.order.bean.OrderBean;
import cn.itcast.order.bean.OrderItemBean;
import cn.itcast.order.bean.ShopBean;
import cn.itcast.order.db.OrderDao;
import cn.itcast.order.utils.CartManager;
import cn.itcast.order.utils.ShopRepository;

/**
 * 订单中心：全部 / 待支付 / 配送中 / 已完成
 * 支持取消订单、去支付、查看详情、再来一单。
 */
public class OrderListActivity extends AppCompatActivity implements OrderCenterAdapter.OnOrderActionListener {

    private static final int TAB_ALL = -1;

    private OrderDao orderDao;
    private OrderCenterAdapter adapter;
    private ListView lvOrder;
    private View layoutEmpty;

    private int currentTab = TAB_ALL;

    private TextView[] tabTexts;
    private View[] tabIndicators;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);

        orderDao = new OrderDao(this);
        currentTab = getIntent().getIntExtra("tab", TAB_ALL);

        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText("我的订单");

        lvOrder = findViewById(R.id.lv_order);
        layoutEmpty = findViewById(R.id.layout_empty);

        adapter = new OrderCenterAdapter(this, this);
        lvOrder.setAdapter(adapter);
        lvOrder.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openDetail((OrderBean) adapter.getItem(position));
            }
        });

        tabTexts = new TextView[]{
                findViewById(R.id.tv_tab_all),
                findViewById(R.id.tv_tab_unpaid),
                findViewById(R.id.tv_tab_delivering),
                findViewById(R.id.tv_tab_done)
        };
        tabIndicators = new View[]{
                findViewById(R.id.indicator_all),
                findViewById(R.id.indicator_unpaid),
                findViewById(R.id.indicator_delivering),
                findViewById(R.id.indicator_done)
        };

        final int[] tabStatus = {TAB_ALL, OrderBean.STATUS_UNPAID,
                OrderBean.STATUS_DELIVERING, OrderBean.STATUS_DONE};
        int[] tabIds = {R.id.tab_all, R.id.tab_unpaid, R.id.tab_delivering, R.id.tab_done};
        for (int i = 0; i < tabIds.length; i++) {
            final int index = i;
            findViewById(tabIds[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentTab = tabStatus[index];
                    updateTabs();
                    refresh();
                }
            });
        }

        findViewById(R.id.tv_go_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(OrderListActivity.this, ShopListActivity.class));
            }
        });

        initBottomNav();
        updateTabs();
    }

    private void initBottomNav() {
        View navHome = findViewById(R.id.nav_home);
        View navOrder = findViewById(R.id.nav_order);
        View navMine = findViewById(R.id.nav_mine);
        navOrder.setSelected(true);
        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OrderListActivity.this, ShopListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
        navMine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(OrderListActivity.this, MineActivity.class));
                finish();
            }
        });
    }

    private void updateTabs() {
        int index = 0;
        if (currentTab == OrderBean.STATUS_UNPAID) {
            index = 1;
        } else if (currentTab == OrderBean.STATUS_DELIVERING) {
            index = 2;
        } else if (currentTab == OrderBean.STATUS_DONE) {
            index = 3;
        }
        for (int i = 0; i < tabTexts.length; i++) {
            boolean selected = i == index;
            tabTexts[i].setSelected(selected);
            tabIndicators[i].setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void refresh() {
        List<OrderBean> list = orderDao.queryByStatus(currentTab);
        adapter.setData(list);
        boolean empty = list.isEmpty();
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        lvOrder.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void openDetail(OrderBean order) {
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra("orderId", order.getId());
        startActivity(intent);
    }

    // ---------------- 条目操作 ----------------

    @Override
    public void onCancel(final OrderBean order) {
        new AlertDialog.Builder(this)
                .setMessage("确定取消这笔订单吗？")
                .setNegativeButton("再想想", null)
                .setPositiveButton("取消订单", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        orderDao.updateStatus(order.getId(), OrderBean.STATUS_CANCEL);
                        refresh();
                        Toast.makeText(OrderListActivity.this, "订单已取消", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    @Override
    public void onPay(OrderBean order) {
        Intent intent = new Intent(this, PayActivity.class);
        intent.putExtra("orderId", order.getId());
        startActivity(intent);
    }

    @Override
    public void onRebuy(OrderBean order) {
        ShopBean shop = ShopRepository.findById(this, order.getShopId());
        if (shop == null) {
            Toast.makeText(this, "店铺已下线，无法再来一单", Toast.LENGTH_SHORT).show();
            return;
        }
        CartManager.switchShop(shop.getId());
        for (OrderItemBean item : order.getItems()) {
            FoodBean food = findFood(shop, item.getFoodId());
            if (food == null) {
                continue;
            }
            for (int i = 0; i < item.getCount(); i++) {
                CartManager.add(food);
            }
        }
        Intent intent = new Intent(this, ShopDetailActivity.class);
        intent.putExtra("shop", shop);
        startActivity(intent);
    }

    @Override
    public void onDetail(OrderBean order) {
        openDetail(order);
    }

    private FoodBean findFood(ShopBean shop, int foodId) {
        for (FoodBean food : shop.getFoodList()) {
            if (food.getFoodId() == foodId) {
                return food;
            }
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }
}
