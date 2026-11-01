package cn.itcast.order.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.itcast.order.R;
import cn.itcast.order.adapter.OrderAdapter;
import cn.itcast.order.bean.FoodBean;
import cn.itcast.order.bean.OrderBean;
import cn.itcast.order.bean.OrderItemBean;
import cn.itcast.order.db.OrderDao;

/**
 * 订单详情：状态、地址、商品清单、金额与订单信息。
 */
public class OrderDetailActivity extends AppCompatActivity {

    private OrderDao orderDao;
    private OrderBean order;

    private View layoutAction;
    private TextView tvCancel;
    private TextView tvPay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        orderDao = new OrderDao(this);
        long orderId = getIntent().getLongExtra("orderId", -1);
        order = orderDao.queryById(orderId);
        if (order == null) {
            finish();
            return;
        }

        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText("订单详情");

        layoutAction = findViewById(R.id.layout_detail_action);
        tvCancel = findViewById(R.id.tv_detail_cancel);
        tvPay = findViewById(R.id.tv_detail_pay);

        bindViews();
        bindActions();
    }

    private void bindViews() {
        TextView tvStatus = findViewById(R.id.tv_detail_status);
        TextView tvStatusTip = findViewById(R.id.tv_detail_status_tip);
        tvStatus.setText(order.getStatusText());
        tvStatusTip.setText(statusTip(order.getStatus()));

        TextView tvAddress = findViewById(R.id.tv_detail_address);
        tvAddress.setText(order.getAddress());
        TextView tvReceiver = findViewById(R.id.tv_detail_receiver);
        tvReceiver.setText(order.getReceiver() + "  " + order.getPhone());
        TextView tvArrive = findViewById(R.id.tv_detail_arrive);
        tvArrive.setText("预计送达：" + order.getArriveTime());

        // 商品清单：订单里存的是快照，转成 FoodBean 复用 OrderAdapter
        List<FoodBean> foods = new ArrayList<>();
        for (OrderItemBean item : order.getItems()) {
            FoodBean food = new FoodBean();
            food.setFoodId(item.getFoodId());
            food.setFoodName(item.getFoodName());
            food.setFoodPic(item.getFoodPic());
            food.setPrice(item.getPrice());
            food.setCount(item.getCount());
            foods.add(food);
        }
        ListView lvOrder = findViewById(R.id.lv_detail_order);
        OrderAdapter adapter = new OrderAdapter(this);
        lvOrder.setAdapter(adapter);
        adapter.setData(foods);

        TextView tvGoods = findViewById(R.id.tv_detail_goods);
        tvGoods.setText("¥" + order.getGoodsPrice().toPlainString());
        TextView tvDistribution = findViewById(R.id.tv_detail_distribution);
        tvDistribution.setText("¥" + order.getDistribution().toPlainString());
        TextView tvDiscount = findViewById(R.id.tv_detail_discount);
        tvDiscount.setText("-¥" + order.getDiscount().toPlainString());
        TextView tvTotal = findViewById(R.id.tv_detail_total);
        tvTotal.setText(order.getTotalText());

        TextView tvOrderNo = findViewById(R.id.tv_detail_order_no);
        tvOrderNo.setText("订单号：" + order.getOrderNo());
        TextView tvCreateTime = findViewById(R.id.tv_detail_create_time);
        tvCreateTime.setText("下单时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.CHINA).format(new Date(order.getCreateTime())));
        TextView tvRemark = findViewById(R.id.tv_detail_remark);
        tvRemark.setText("备注：" + (order.getRemark().isEmpty() ? "无" : order.getRemark()));
        TextView tvTableware = findViewById(R.id.tv_detail_tableware);
        tvTableware.setText("餐具：" + order.getTableware() + " 份");
    }

    private void bindActions() {
        boolean unpaid = order.getStatus() == OrderBean.STATUS_UNPAID;
        boolean delivering = order.getStatus() == OrderBean.STATUS_DELIVERING;

        if (!unpaid && !delivering) {
            layoutAction.setVisibility(View.GONE);
            return;
        }

        tvPay.setVisibility(unpaid ? View.VISIBLE : View.GONE);
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmCancel();
            }
        });
        tvPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OrderDetailActivity.this, PayActivity.class);
                intent.putExtra("orderId", order.getId());
                startActivity(intent);
            }
        });
    }

    private void confirmCancel() {
        new AlertDialog.Builder(this)
                .setMessage("确定取消这笔订单吗？")
                .setNegativeButton("再想想", null)
                .setPositiveButton("取消订单", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        orderDao.updateStatus(order.getId(), OrderBean.STATUS_CANCEL);
                        finish();
                    }
                })
                .show();
    }

    private String statusTip(int status) {
        switch (status) {
            case OrderBean.STATUS_UNPAID:
                return "订单已生成，请尽快完成支付";
            case OrderBean.STATUS_DELIVERING:
                return "骑手正在取餐，请耐心等待";
            case OrderBean.STATUS_DONE:
                return "订单已完成，感谢你的支持";
            case OrderBean.STATUS_CANCEL:
                return "订单已取消，欢迎再次光临";
            default:
                return "";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        order = orderDao.queryById(order.getId());
        if (order != null) {
            bindViews();
        }
    }
}
