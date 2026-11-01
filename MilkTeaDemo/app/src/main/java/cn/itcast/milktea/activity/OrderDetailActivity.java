package cn.itcast.milktea.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;

import cn.itcast.milktea.R;
import cn.itcast.milktea.bean.OrderBean;
import cn.itcast.milktea.bean.OrderItemBean;
import cn.itcast.milktea.db.OrderDao;
import cn.itcast.milktea.utils.PriceUtils;

/**
 * 订单详情页：状态 + 取餐码/地址 + 商品明细 + 金额 + 订单信息。
 *
 * 数据一律从数据库按订单号重新查，不通过 Intent 传整个 Bean：
 * 序列化对象容易在字段变动后出现"传过去是旧数据"的问题，按 id 查库永远是最新的。
 */
public class OrderDetailActivity extends AppCompatActivity {

    private OrderBean order;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        String orderNo = getIntent().getStringExtra("orderNo");
        order = orderNo == null ? null : new OrderDao(this).queryByOrderNo(orderNo);
        if (order == null) {
            Toast.makeText(this, "订单不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initTitleBar();
        bindStatus();
        bindCodeOrAddress();
        bindGoods();
        bindAmount();
        bindOrderInfo();
    }

    private void initTitleBar() {
        ((TextView) findViewById(R.id.tv_title)).setText(R.string.title_order_detail);
        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /** 大状态文字 + 一句引导语 */
    private void bindStatus() {
        ((TextView) findViewById(R.id.tv_detail_status)).setText(order.getStatusText());

        String hint;
        switch (order.getStatus()) {
            case OrderBean.STATUS_UNPAID:
                hint = "订单已提交，请尽快完成支付";
                break;
            case OrderBean.STATUS_MAKING:
                hint = order.getMode() == OrderBean.MODE_PICKUP
                        ? "正在制作，请凭取餐码到店取餐"
                        : "正在制作，骑手取餐后马上送到";
                break;
            case OrderBean.STATUS_DONE:
                hint = "订单已完成，感谢光临，期待下次再见";
                break;
            case OrderBean.STATUS_CANCELED:
                hint = "订单已取消，欢迎重新下单";
                break;
            default:
                hint = "";
        }
        ((TextView) findViewById(R.id.tv_detail_status_hint)).setText(hint);
    }

    /** 自取显示取餐码，外送显示收货地址，两者互斥 */
    private void bindCodeOrAddress() {
        View codeCard = findViewById(R.id.ll_detail_code);
        View addressCard = findViewById(R.id.ll_detail_address);

        if (order.getMode() == OrderBean.MODE_DELIVERY) {
            codeCard.setVisibility(View.GONE);
            addressCard.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tv_detail_address)).setText(order.getAddress());
        } else {
            codeCard.setVisibility(View.VISIBLE);
            addressCard.setVisibility(View.GONE);
            ((TextView) findViewById(R.id.tv_detail_code)).setText(order.getPickupCode());
        }
    }

    private void bindGoods() {
        LinearLayout container = findViewById(R.id.ll_detail_goods);
        container.removeAllViews();
        for (OrderItemBean item : order.getItems()) {
            View row = LayoutInflater.from(this)
                    .inflate(R.layout.item_order_goods, container, false);
            ((TextView) row.findViewById(R.id.tv_og_name)).setText(item.getName());
            ((TextView) row.findViewById(R.id.tv_og_spec)).setText(item.getSpecText());
            ((TextView) row.findViewById(R.id.tv_og_count)).setText("x" + item.getCount());
            ((TextView) row.findViewById(R.id.tv_og_subtotal)).setText(PriceUtils.format(
                    item.getUnitPrice().multiply(new BigDecimal(item.getCount()))));
            container.addView(row);
        }

        TextView tvRemark = findViewById(R.id.tv_detail_remark);
        String remark = order.getRemark();
        if (remark == null || remark.trim().isEmpty()) {
            tvRemark.setVisibility(View.GONE);
        } else {
            tvRemark.setVisibility(View.VISIBLE);
            tvRemark.setText("备注：" + remark);
        }
    }

    private void bindAmount() {
        ((TextView) findViewById(R.id.tv_detail_goods_total))
                .setText(PriceUtils.format(order.getGoodsTotal()));

        BigDecimal extra = safe(order.getPackingFee()).add(safe(order.getDeliveryFee()));
        ((TextView) findViewById(R.id.tv_detail_extra_fee)).setText(PriceUtils.format(extra));

        TextView tvDiscount = findViewById(R.id.tv_detail_discount);
        tvDiscount.setText(order.getDiscount() != null
                && order.getDiscount().compareTo(BigDecimal.ZERO) > 0
                ? "-" + PriceUtils.format(order.getDiscount()) : "无");

        ((TextView) findViewById(R.id.tv_detail_total))
                .setText(PriceUtils.format(order.getTotal()));
    }

    private void bindOrderInfo() {
        ((TextView) findViewById(R.id.tv_detail_orderno))
                .setText("订单号：" + order.getOrderNo());
        ((TextView) findViewById(R.id.tv_detail_time))
                .setText("下单时间：" + PriceUtils.formatTime(order.getCreatedAt()));
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
