package cn.itcast.order.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.itcast.order.R;
import cn.itcast.order.adapter.OrderAdapter;
import cn.itcast.order.bean.AddressBean;
import cn.itcast.order.bean.CouponBean;
import cn.itcast.order.bean.FoodBean;
import cn.itcast.order.bean.OrderBean;
import cn.itcast.order.bean.OrderItemBean;
import cn.itcast.order.bean.ShopBean;
import cn.itcast.order.db.OrderDao;
import cn.itcast.order.utils.AppPrefs;
import cn.itcast.order.utils.CartManager;
import cn.itcast.order.utils.CouponUtils;
import cn.itcast.order.view.FlowLayout;

/**
 * 确认订单界面（对应教材 12.7 订单模块，已扩展为完整结算页）
 *
 * 新增：收货地址选择、送达时间、优惠券、备注快捷标签、餐具份数。
 * 点击“提交订单”会先在本地数据库生成一笔待支付订单，再进入支付页。
 */
public class OrderActivity extends AppCompatActivity {

    private static final int REQ_ADDRESS = 1001;
    private static final int REQ_COUPON = 1002;

    private static final String[] REMARK_TAGS = {
            "无接触配送", "不要辣", "少放盐", "多放醋", "无需餐具", "请尽快送达"
    };

    private ShopBean shop;
    private List<FoodBean> orderList;
    private OrderAdapter orderAdapter;

    private BigDecimal goodsPrice = BigDecimal.ZERO;
    private BigDecimal distribution = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;

    private AddressBean address;
    private CouponBean selectedCoupon;
    /** 用户手动选过优惠券后，不再自动挑券 */
    private boolean couponChosen = false;

    private String arriveTime = "立即送出";
    private int tableware = 1;

    private TextView tvAddress;
    private TextView tvReceiver;
    private TextView tvArriveTime;
    private TextView tvCoupon;
    private TextView tvDiscount;
    private TextView tvTotal;
    private TextView tvPayTotal;
    private TextView tvPayCount;
    private TextView tvTableware;
    private EditText etRemark;

    private OrderDao orderDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        shop = (ShopBean) getIntent().getSerializableExtra("shop");
        orderList = CartManager.getCartList();
        if (orderList.isEmpty()) {
            Toast.makeText(this, "购物车是空的，无法下单", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        orderDao = new OrderDao(this);
        AppPrefs.ensureDefaultAddress(this);

        goodsPrice = CartManager.getTotalPrice();
        if (shop != null && shop.getDistributionCost() != null) {
            distribution = shop.getDistributionCost();
        }
        selectedCoupon = CouponUtils.bestCoupon(shop, goodsPrice);
        discount = selectedCoupon == null ? BigDecimal.ZERO : selectedCoupon.getAmount();

        initView();
        recalc();
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
        tvTitle.setText(R.string.order_confirm);

        tvAddress = findViewById(R.id.tv_address);
        tvReceiver = findViewById(R.id.tv_receiver);
        tvArriveTime = findViewById(R.id.tv_arrive_time);
        tvCoupon = findViewById(R.id.tv_coupon);
        tvDiscount = findViewById(R.id.tv_order_discount);
        tvTotal = findViewById(R.id.tv_order_total);
        tvPayTotal = findViewById(R.id.tv_pay_total);
        tvPayCount = findViewById(R.id.tv_pay_count);
        tvTableware = findViewById(R.id.tv_tableware);
        etRemark = findViewById(R.id.et_remark);

        // 地址
        findViewById(R.id.layout_address).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OrderActivity.this, AddressListActivity.class);
                intent.putExtra("select_mode", true);
                startActivityForResult(intent, REQ_ADDRESS);
            }
        });

        // 送达时间
        findViewById(R.id.layout_time).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseArriveTime();
            }
        });

        // 优惠券
        findViewById(R.id.layout_coupon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OrderActivity.this, CouponActivity.class);
                intent.putExtra("shop", shop);
                intent.putExtra("goodsPrice", goodsPrice.toPlainString());
                startActivityForResult(intent, REQ_COUPON);
            }
        });

        // 已选商品
        ListView lvOrder = findViewById(R.id.lv_order);
        orderAdapter = new OrderAdapter(this);
        lvOrder.setAdapter(orderAdapter);
        orderAdapter.setData(orderList);

        // 备注快捷标签
        FlowLayout flowRemark = findViewById(R.id.flow_remark);
        flowRemark.setSpacing(dp(8), dp(8));
        for (final String tag : REMARK_TAGS) {
            TextView tv = new TextView(this);
            tv.setText(tag);
            tv.setTextSize(12);
            tv.setTextColor(getResources().getColor(R.color.text_gray));
            tv.setBackgroundResource(R.drawable.bg_tag_gray);
            tv.setPadding(dp(10), dp(5), dp(10), dp(5));
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    appendRemark(tag);
                }
            });
            flowRemark.addView(tv);
        }

        // 餐具
        findViewById(R.id.tv_tableware_minus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tableware > 0) {
                    tableware--;
                    tvTableware.setText(String.valueOf(tableware));
                }
            }
        });
        findViewById(R.id.tv_tableware_plus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tableware < 10) {
                    tableware++;
                    tvTableware.setText(String.valueOf(tableware));
                }
            }
        });

        // 提交订单
        findViewById(R.id.tv_go_pay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitOrder();
            }
        });
    }

    /** 把快捷标签拼到备注输入框 */
    private void appendRemark(String tag) {
        String current = etRemark.getText().toString().trim();
        if (current.contains(tag)) {
            return;
        }
        if (current.isEmpty()) {
            etRemark.setText(tag);
        } else {
            etRemark.setText(current + "，" + tag);
        }
        etRemark.setSelection(etRemark.getText().length());
    }

    /** 送达时间选择 */
    private void chooseArriveTime() {
        final String[] items = {"立即送出（约" + (shop == null ? "30" : shop.getTime()) + "）",
                "30 分钟后送达", "60 分钟后送达"};
        new AlertDialog.Builder(this)
                .setTitle("选择送达时间")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            arriveTime = "立即送出";
                        } else {
                            Calendar calendar = Calendar.getInstance();
                            calendar.add(Calendar.MINUTE, which == 1 ? 30 : 60);
                            arriveTime = new SimpleDateFormat("HH:mm", Locale.CHINA)
                                    .format(calendar.getTime()) + " 送达";
                        }
                        tvArriveTime.setText(arriveTime);
                    }
                })
                .show();
    }

    /** 重新计算金额与底部栏 */
    private void recalc() {
        discount = CouponUtils.safeDiscount(discount, goodsPrice);
        total = goodsPrice.add(distribution).subtract(discount).setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        TextView tvGoods = findViewById(R.id.tv_order_goods_price);
        TextView tvDist = findViewById(R.id.tv_order_distribution);
        tvGoods.setText("¥" + money(goodsPrice));
        tvDist.setText("¥" + money(distribution));
        tvDiscount.setText("-¥" + money(discount));
        tvTotal.setText("¥" + money(total));
        tvPayTotal.setText("合计 ¥" + money(total));
        tvPayCount.setText("共" + CartManager.getTotalCount() + "件商品");

        // 地址
        address = AppPrefs.getDefaultAddress(this);
        if (address == null) {
            tvAddress.setText("请选择收货地址");
            tvReceiver.setText("");
        } else {
            tvAddress.setText(address.getDetail());
            tvReceiver.setText(address.getName() + "  " + address.getPhone());
        }

        // 优惠券文案
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            String title = selectedCoupon == null ? "已优惠" : selectedCoupon.getTitle();
            tvCoupon.setText("-¥" + money(discount) + "（" + title + "）");
        } else {
            tvCoupon.setText("暂无可用");
        }
    }

    /** 生成订单并进入支付页 */
    private void submitOrder() {
        if (address == null) {
            Toast.makeText(this, "请先选择收货地址", Toast.LENGTH_SHORT).show();
            return;
        }
        if (shop == null) {
            Toast.makeText(this, "店铺信息异常", Toast.LENGTH_SHORT).show();
            return;
        }

        OrderBean order = new OrderBean();
        order.setOrderNo("MT" + new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA)
                .format(new Date()) + (int) (Math.random() * 900 + 100));
        order.setShopId(shop.getId());
        order.setShopName(shop.getShopName());
        order.setShopBanner(shop.getBanner());
        order.setGoodsPrice(goodsPrice);
        order.setDistribution(distribution);
        order.setDiscount(discount);
        order.setTotal(total);
        order.setAddress(address.getDetail());
        order.setReceiver(address.getName());
        order.setPhone(address.getPhone());
        order.setRemark(etRemark.getText().toString().trim());
        order.setTableware(tableware);
        order.setArriveTime(arriveTime);
        order.setStatus(OrderBean.STATUS_UNPAID);
        order.setCreateTime(System.currentTimeMillis());

        List<OrderItemBean> items = new ArrayList<>();
        for (FoodBean food : orderList) {
            items.add(new OrderItemBean(food.getFoodId(), food.getFoodName(),
                    food.getFoodPic(), food.getPrice(), food.getCount()));
        }
        order.setItems(items);

        long orderId = orderDao.insert(order);
        if (orderId <= 0) {
            Toast.makeText(this, "下单失败，请重试", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PayActivity.class);
        intent.putExtra("orderId", orderId);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        if (requestCode == REQ_ADDRESS) {
            AddressBean bean = (AddressBean) data.getSerializableExtra("address");
            if (bean != null) {
                address = bean;
                tvAddress.setText(bean.getDetail());
                tvReceiver.setText(bean.getName() + "  " + bean.getPhone());
            }
        } else if (requestCode == REQ_COUPON) {
            String value = data.getStringExtra("discount");
            String title = data.getStringExtra("couponTitle");
            couponChosen = true;
            try {
                discount = value == null ? BigDecimal.ZERO : new BigDecimal(value);
            } catch (NumberFormatException e) {
                discount = BigDecimal.ZERO;
            }
            selectedCoupon = null;
            if (discount.compareTo(BigDecimal.ZERO) > 0 && title != null) {
                selectedCoupon = new CouponBean();
                selectedCoupon.setTitle(title);
            }
            recalc();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        recalc();
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
