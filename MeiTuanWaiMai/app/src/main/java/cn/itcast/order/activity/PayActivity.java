package cn.itcast.order.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import cn.itcast.order.R;
import cn.itcast.order.bean.OrderBean;
import cn.itcast.order.db.OrderDao;
import cn.itcast.order.utils.CartManager;

/**
 * 支付界面（对应教材 12.7.3 搭建支付界面）
 *
 * 从订单确认页或订单中心进入，携带 orderId；
 * 支付成功后把订单状态改为“配送中”，清空购物车并进入支付成功页。
 */
public class PayActivity extends AppCompatActivity {

    private String total = "0.00";
    private String shopName = "";
    private long orderId = -1;

    private OrderDao orderDao;
    private OrderBean order;

    private TextView tvPayTypeTip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay);

        orderDao = new OrderDao(this);
        orderId = getIntent().getLongExtra("orderId", -1);
        if (orderId > 0) {
            order = orderDao.queryById(orderId);
            if (order != null) {
                total = order.getTotal().toPlainString();
                shopName = order.getShopName();
            }
        }
        if ("0.00".equals(total)) {
            String extra = getIntent().getStringExtra("total");
            if (extra != null) {
                total = extra;
            }
            String name = getIntent().getStringExtra("shopName");
            if (name != null) {
                shopName = name;
            }
        }

        initView();
    }

    private void initView() {
        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(R.string.pay_order);

        TextView tvAmount = findViewById(R.id.tv_pay_amount);
        tvAmount.setText("¥" + total);
        TextView tvPayShop = findViewById(R.id.tv_pay_shop);
        tvPayShop.setText(shopName);
        TextView tvPayReal = findViewById(R.id.tv_pay_real);
        tvPayReal.setText("实付 ¥" + total);

        tvPayTypeTip = findViewById(R.id.tv_pay_type_tip);

        RadioGroup rgPayType = findViewById(R.id.rg_pay_type);
        rgPayType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_alipay) {
                    tvPayTypeTip.setText("支付宝");
                } else if (checkedId == R.id.rb_balance) {
                    tvPayTypeTip.setText("余额支付");
                } else {
                    tvPayTypeTip.setText("微信支付");
                }
            }
        });

        findViewById(R.id.tv_confirm_pay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPay();
            }
        });
    }

    /** 模拟支付：更新订单状态 → 清空购物车 → 成功页 */
    private void doPay() {
        if (order == null) {
            new AlertDialog.Builder(this)
                    .setTitle("支付失败")
                    .setMessage("订单信息不存在，请返回重新下单")
                    .setPositiveButton("知道了", null)
                    .show();
            return;
        }
        orderDao.updateStatus(orderId, OrderBean.STATUS_DELIVERING);
        CartManager.clear();

        Intent intent = new Intent(this, PaySuccessActivity.class);
        intent.putExtra("orderId", orderId);
        intent.putExtra("total", total);
        intent.putExtra("shopName", shopName);
        startActivity(intent);
        finish();
    }
}
