package cn.itcast.order.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import cn.itcast.order.R;

/**
 * 支付成功页
 */
public class PaySuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_success);

        final long orderId = getIntent().getLongExtra("orderId", -1);
        String total = getIntent().getStringExtra("total");
        String shopName = getIntent().getStringExtra("shopName");
        if (total == null) {
            total = "0.00";
        }

        TextView tvAmount = findViewById(R.id.tv_success_amount);
        tvAmount.setText("¥" + total);
        TextView tvTip = findViewById(R.id.tv_success_tip);
        tvTip.setText((shopName == null ? "" : shopName) + " 正在为你备餐\n预计 30 分钟送达");

        findViewById(R.id.tv_view_order).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PaySuccessActivity.this, OrderDetailActivity.class);
                intent.putExtra("orderId", orderId);
                startActivity(intent);
                finish();
            }
        });

        findViewById(R.id.tv_back_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PaySuccessActivity.this, ShopListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, ShopListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
