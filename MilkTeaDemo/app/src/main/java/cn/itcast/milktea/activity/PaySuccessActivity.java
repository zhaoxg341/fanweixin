package cn.itcast.milktea.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import cn.itcast.milktea.R;
import cn.itcast.milktea.bean.OrderBean;
import cn.itcast.milktea.db.OrderDao;
import cn.itcast.milktea.utils.PriceUtils;

/**
 * 支付成功页：大对勾 + 实付金额 + 取餐码。
 *
 * 这一页刻意不放到返回栈的中间层：点"继续点单"直接清掉上面所有页面回主页，
 * 避免用户按返回键又退回收银台重复支付。
 */
public class PaySuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_success);

        String orderNo = getIntent().getStringExtra("orderNo");
        OrderBean order = orderNo == null ? null : new OrderDao(this).queryByOrderNo(orderNo);
        if (order == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tv_success_amount))
                .setText("实付 " + PriceUtils.format(order.getTotal()));

        TextView tvCodeTitle = findViewById(R.id.tv_code_title);
        TextView tvCode = findViewById(R.id.tv_success_code);
        TextView tvHint = findViewById(R.id.tv_success_hint);

        if (order.getMode() == OrderBean.MODE_DELIVERY) {
            // 外送没有取餐码，用订单尾号代替，并提示等骑手
            tvCodeTitle.setText("订单尾号");
            String no = order.getOrderNo();
            tvCode.setText(no.substring(no.length() - 4));
            tvHint.setText("骑手正在赶来，请保持电话畅通");
        } else {
            tvCodeTitle.setText(R.string.pickup_code_title);
            tvCode.setText(order.getPickupCode());
            tvHint.setText("请凭取餐码到店取餐");
        }

        findViewById(R.id.tv_success_view_order).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PaySuccessActivity.this, OrderDetailActivity.class);
                intent.putExtra("orderNo", orderNo);
                startActivity(intent);
                finish();
            }
        });

        findViewById(R.id.tv_success_back_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToHome();
            }
        });
    }

    @Override
    public void onBackPressed() {
        backToHome();
    }

    /** 清掉中间页面，直接回到主界面 */
    private void backToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
