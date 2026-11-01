package cn.itcast.milktea.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;

import cn.itcast.milktea.R;
import cn.itcast.milktea.adapter.CartAdapter;
import cn.itcast.milktea.utils.CartManager;
import cn.itcast.milktea.utils.GoodsDataUtils;
import cn.itcast.milktea.utils.PriceUtils;

/**
 * 购物车页：改数量、清空、看满减，去结算。
 */
public class CartActivity extends AppCompatActivity {

    private CartAdapter adapter;
    private ListView lvCart;
    private View emptyView;
    private TextView tvTotal;
    private TextView tvDiscount;
    private TextView tvSettle;
    private TextView tvCouponTip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        TextView title = findViewById(R.id.tv_title);
        title.setText(R.string.title_cart);
        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        lvCart = findViewById(R.id.lv_cart);
        emptyView = findViewById(R.id.ll_cart_empty);
        tvTotal = findViewById(R.id.tv_cart_total);
        tvDiscount = findViewById(R.id.tv_cart_discount);
        tvSettle = findViewById(R.id.tv_cart_settle);
        tvCouponTip = findViewById(R.id.tv_cart_coupon_tip);

        adapter = new CartAdapter(this, new CartAdapter.OnCartChangedListener() {
            @Override
            public void onCartChanged() {
                refresh();
            }
        });
        lvCart.setAdapter(adapter);

        findViewById(R.id.tv_cart_go_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.tv_cart_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!CartManager.isEmpty()) {
                    showClearDialog();
                }
            }
        });
        tvSettle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CartManager.isEmpty()) {
                    return;
                }
                startActivity(new Intent(CartActivity.this, ConfirmOrderActivity.class));
            }
        });

        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean empty = CartManager.isEmpty();
        adapter.setData(CartManager.getCartList());
        lvCart.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);

        BigDecimal goodsTotal = CartManager.getGoodsTotal();
        BigDecimal discount = GoodsDataUtils.couponDiscount(this, goodsTotal);
        tvTotal.setText(PriceUtils.format(goodsTotal));
        tvDiscount.setText(discount.compareTo(BigDecimal.ZERO) > 0
                ? "已优惠 " + PriceUtils.format(discount)
                : "满减：" + GoodsDataUtils.couponText(this));
        tvCouponTip.setText(discount.compareTo(BigDecimal.ZERO) > 0
                ? "已享受满减优惠 " + PriceUtils.format(discount) + "，结算时自动抵扣"
                : GoodsDataUtils.nextCouponTip(this, goodsTotal));
        tvSettle.setBackgroundResource(empty
                ? R.drawable.bg_btn_gray : R.drawable.bg_btn_primary_selector);
    }

    /** 清空购物车确认弹窗（自绘布局，和整体风格一致） */
    private void showClearDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_clear_cart, null);
        final AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        view.findViewById(R.id.tv_dialog_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        view.findViewById(R.id.tv_dialog_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CartManager.clear();
                refresh();
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}
