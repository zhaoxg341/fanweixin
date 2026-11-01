package cn.itcast.milktea.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cn.itcast.milktea.R;
import cn.itcast.milktea.bean.OrderBean;
import cn.itcast.milktea.db.OrderDao;
import cn.itcast.milktea.utils.PriceUtils;

/**
 * 收银台（模拟支付）。
 *
 * 支付方式只是"选一个高亮"，不接真实支付 SDK。
 * 点"立即支付"把订单状态从"待支付"改成"制作中"，然后进成功页。
 *
 * 注意购物车是在"提交订单"那一刻清空的（订单已落库），
 * 不能放在这里清 —— 因为从订单列表进来支付一笔旧订单时，
 * 用户可能正在重新挑东西，清掉就把新选的一起删了。
 */
public class PayActivity extends AppCompatActivity {

    private static final int PAY_WECHAT = 0;
    private static final int PAY_BALANCE = 1;

    private int payMethod = PAY_WECHAT;

    private ImageView ivCheckWechat;
    private ImageView ivCheckBalance;

    private OrderBean order;
    private OrderDao orderDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay);

        String orderNo = getIntent().getStringExtra("orderNo");
        orderDao = new OrderDao(this);
        order = orderNo == null ? null : orderDao.queryByOrderNo(orderNo);
        if (order == null) {
            Toast.makeText(this, "订单不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initTitleBar();
        bindOrder();
        initPayMethod();

        findViewById(R.id.tv_pay_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPay();
            }
        });
    }

    private void initTitleBar() {
        ((TextView) findViewById(R.id.tv_title)).setText(R.string.title_pay);
        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 返回不取消订单，订单还在"待支付"，可以去订单列表里继续付
                backToOrders();
            }
        });
    }

    private void bindOrder() {
        ((TextView) findViewById(R.id.tv_pay_amount)).setText(PriceUtils.format(order.getTotal()));

        String modeText = order.getMode() == OrderBean.MODE_PICKUP
                ? getString(R.string.pickup_in_shop) : getString(R.string.delivery);
        ((TextView) findViewById(R.id.tv_pay_summary))
                .setText("共 " + order.getItemCount() + " 件 · " + modeText);
    }

    private void initPayMethod() {
        ivCheckWechat = findViewById(R.id.iv_check_wechat);
        ivCheckBalance = findViewById(R.id.iv_check_balance);

        findViewById(R.id.row_pay_wechat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payMethod = PAY_WECHAT;
                refreshCheck();
            }
        });
        findViewById(R.id.row_pay_balance).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payMethod = PAY_BALANCE;
                refreshCheck();
            }
        });
        refreshCheck();
    }

    private void refreshCheck() {
        boolean wechat = payMethod == PAY_WECHAT;
        ivCheckWechat.setImageResource(wechat
                ? R.drawable.ic_selected : R.drawable.ic_unselected);
        ivCheckBalance.setImageResource(wechat
                ? R.drawable.ic_unselected : R.drawable.ic_selected);
    }

    private void doPay() {
        // 已经支付过的订单（比如从订单列表重复进来）直接放行，不重复改状态
        if (order.getStatus() == OrderBean.STATUS_UNPAID) {
            orderDao.updateStatus(order.getOrderNo(), OrderBean.STATUS_MAKING);
        }

        Intent intent = new Intent(this, PaySuccessActivity.class);
        intent.putExtra("orderNo", order.getOrderNo());
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        backToOrders();
    }

    /** 回到主界面的"订单"Tab，让用户能看到这笔待支付订单 */
    private void backToOrders() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("tab", MainActivity.TAB_ORDERS);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
