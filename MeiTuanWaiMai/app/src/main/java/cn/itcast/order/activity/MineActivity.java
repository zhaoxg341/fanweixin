package cn.itcast.order.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import cn.itcast.order.R;
import cn.itcast.order.bean.OrderBean;
import cn.itcast.order.db.OrderDao;
import cn.itcast.order.utils.AppPrefs;

/**
 * 我的：个人信息、订单快捷入口、地址 / 收藏 / 优惠券 / 关于
 */
public class MineActivity extends AppCompatActivity {

    private TextView tvNickname;
    private TextView tvAvatar;
    private TextView tvCountUnpaid;
    private TextView tvCountDelivering;
    private TextView tvCountDone;

    private OrderDao orderDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mine);

        orderDao = new OrderDao(this);

        tvNickname = findViewById(R.id.tv_nickname);
        tvAvatar = findViewById(R.id.tv_avatar);
        tvCountUnpaid = findViewById(R.id.tv_count_unpaid);
        tvCountDelivering = findViewById(R.id.tv_count_delivering);
        tvCountDone = findViewById(R.id.tv_count_done);

        findViewById(R.id.layout_profile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editNickname();
            }
        });

        findViewById(R.id.entry_unpaid).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOrders(OrderBean.STATUS_UNPAID);
            }
        });
        findViewById(R.id.entry_delivering).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOrders(OrderBean.STATUS_DELIVERING);
            }
        });
        findViewById(R.id.entry_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOrders(OrderBean.STATUS_DONE);
            }
        });
        findViewById(R.id.layout_all_orders).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOrders(-1);
            }
        });

        findViewById(R.id.layout_address).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppPrefs.ensureDefaultAddress(MineActivity.this);
                startActivity(new Intent(MineActivity.this, AddressListActivity.class));
            }
        });
        findViewById(R.id.layout_favorite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MineActivity.this, FavoriteActivity.class));
            }
        });
        findViewById(R.id.layout_coupon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MineActivity.this, CouponActivity.class));
            }
        });
        findViewById(R.id.layout_about).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MineActivity.this)
                        .setTitle("关于美团外卖")
                        .setMessage("美团外卖课程设计演示版\n\n"
                                + "功能包括：店铺列表、分类频道、搜索、排序筛选、"
                                + "店铺评价、购物车、优惠券、地址管理、订单中心与支付流程。\n\n"
                                + "数据保存在本地 SQLite 与 SharedPreferences 中，仅用于学习。")
                        .setPositiveButton("知道了", null)
                        .show();
            }
        });

        initBottomNav();
    }

    private void initBottomNav() {
        View navHome = findViewById(R.id.nav_home);
        View navOrder = findViewById(R.id.nav_order);
        View navMine = findViewById(R.id.nav_mine);
        navMine.setSelected(true);
        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MineActivity.this, ShopListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
        navOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOrders(-1);
                finish();
            }
        });
    }

    private void openOrders(int status) {
        Intent intent = new Intent(this, OrderListActivity.class);
        intent.putExtra("tab", status);
        startActivity(intent);
    }

    /** 修改昵称，同步刷新头像首字 */
    private void editNickname() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(AppPrefs.getNickname(this));
        input.setSelection(input.getText().length());
        input.setHint("请输入昵称");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("修改昵称")
                .setView(container)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) {
                            Toast.makeText(MineActivity.this, "昵称不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        AppPrefs.setNickname(MineActivity.this, name);
                        refreshProfile();
                    }
                })
                .show();
    }

    private void refreshProfile() {
        String nickname = AppPrefs.getNickname(this);
        tvNickname.setText(nickname);
        tvAvatar.setText(nickname.isEmpty() ? "美" : nickname.substring(0, 1));
        TextView tvPhone = findViewById(R.id.tv_phone);
        tvPhone.setText(AppPrefs.getPhone(this));

        tvCountUnpaid.setText(String.valueOf(orderDao.countByStatus(OrderBean.STATUS_UNPAID)));
        tvCountDelivering.setText(String.valueOf(orderDao.countByStatus(OrderBean.STATUS_DELIVERING)));
        tvCountDone.setText(String.valueOf(orderDao.countByStatus(OrderBean.STATUS_DONE)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProfile();
    }
}
