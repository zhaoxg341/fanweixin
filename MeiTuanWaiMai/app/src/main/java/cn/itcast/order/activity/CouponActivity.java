package cn.itcast.order.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import cn.itcast.order.R;
import cn.itcast.order.adapter.CouponAdapter;
import cn.itcast.order.bean.CouponBean;
import cn.itcast.order.bean.ShopBean;
import cn.itcast.order.utils.CouponUtils;

/**
 * 选择优惠券。点击可用券后把优惠金额回传给结算页。
 */
public class CouponActivity extends AppCompatActivity {

    private List<CouponBean> coupons;
    private BigDecimal goodsPrice = BigDecimal.ZERO;
    /** 浏览模式：从“我的-优惠券”进入，只展示不下单 */
    private boolean browseMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coupon);

        ShopBean shop = (ShopBean) getIntent().getSerializableExtra("shop");
        browseMode = shop == null;
        String price = getIntent().getStringExtra("goodsPrice");
        try {
            goodsPrice = price == null ? BigDecimal.ZERO : new BigDecimal(price);
        } catch (NumberFormatException e) {
            goodsPrice = BigDecimal.ZERO;
        }

        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText("选择优惠券");

        coupons = browseMode ? buildBrowseCoupons() : CouponUtils.buildCoupons(shop, goodsPrice);
        CouponAdapter adapter = new CouponAdapter(this);
        ListView lvCoupon = findViewById(R.id.lv_coupon);
        lvCoupon.setAdapter(adapter);
        adapter.setData(coupons);

        lvCoupon.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CouponBean coupon = coupons.get(position);
                if (!coupon.isUsable()) {
                    Toast.makeText(CouponActivity.this, coupon.getUnusableText(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (browseMode) {
                    Toast.makeText(CouponActivity.this,
                            "该券将在下单时自动抵扣", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent data = new Intent();
                data.putExtra("discount", coupon.getAmount().toPlainString());
                data.putExtra("couponTitle", coupon.getTitle());
                setResult(RESULT_OK, data);
                finish();
            }
        });

        findViewById(R.id.tv_no_coupon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent data = new Intent();
                data.putExtra("discount", "0");
                data.putExtra("couponTitle", "");
                setResult(RESULT_OK, data);
                finish();
            }
        });

        if (browseMode) {
            findViewById(R.id.tv_no_coupon).setVisibility(View.GONE);
            TextView tip = findViewById(R.id.tv_coupon_tip);
            tip.setText("以下为你的优惠券，下单时自动抵扣");
            tip.setVisibility(View.VISIBLE);
        }
        if (coupons.isEmpty()) {
            findViewById(R.id.tv_coupon_tip).setVisibility(View.GONE);
        }
    }

    /** “我的优惠券”展示用：几张通用券 */
    private List<CouponBean> buildBrowseCoupons() {
        List<CouponBean> list = new ArrayList<>();
        list.add(new CouponBean("browse_1", "满30减10", "通用券",
                new BigDecimal("30"), new BigDecimal("10")));
        list.add(new CouponBean("browse_2", "满50减8", "平台券",
                new BigDecimal("50"), new BigDecimal("8")));
        list.add(new CouponBean("browse_3", "满100减20", "通用券",
                new BigDecimal("100"), new BigDecimal("20")));
        return list;
    }
}
