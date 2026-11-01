package cn.itcast.milktea.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import cn.itcast.milktea.R;
import cn.itcast.milktea.bean.CartItem;
import cn.itcast.milktea.bean.OrderBean;
import cn.itcast.milktea.bean.OrderItemBean;
import cn.itcast.milktea.bean.ShopBean;
import cn.itcast.milktea.db.OrderDao;
import cn.itcast.milktea.utils.AppPrefs;
import cn.itcast.milktea.utils.CartManager;
import cn.itcast.milktea.utils.GoodsDataUtils;
import cn.itcast.milktea.utils.PriceUtils;

/**
 * 确认订单页。
 *
 * 这一页是"购物车 → 订单"的转换点，做三件事：
 *   1. 选到店自取还是外卖配送（影响配送费、地址是否显示）；
 *   2. 把金额算清楚：商品小计 + 打包费 + 配送费 - 满减；
 *   3. 点"提交订单"时把购物车快照成一张订单写进数据库（状态=待支付），再跳收银台。
 *
 * 注意这里只是"下单"，不是"付款完成"：订单先以 STATUS_UNPAID 落库，
 * 真正付款成功后由 PayActivity 把状态改成"制作中"，这样中途退出也能在订单列表里继续支付。
 */
public class ConfirmOrderActivity extends AppCompatActivity {

    private ShopBean shop;
    private int mode = OrderBean.MODE_PICKUP;

    private BigDecimal goodsTotal = BigDecimal.ZERO;
    private BigDecimal packing = BigDecimal.ZERO;
    private BigDecimal delivery = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;

    private TextView tvModePickup;
    private TextView tvModeDelivery;
    private TextView tvTotal;
    private TextView tvCoupon;
    private TextView tvDiscount;
    private LinearLayout llAddress;
    private EditText etRemark;

    private OrderDao orderDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 购物车空了就没必要停留在这一页（比如连点两次提交）
        if (CartManager.isEmpty()) {
            Toast.makeText(this, R.string.cart_empty_tip, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_confirm_order);

        orderDao = new OrderDao(this);
        tvTotal = findViewById(R.id.tv_confirm_total);
        tvCoupon = findViewById(R.id.tv_confirm_coupon);
        tvDiscount = findViewById(R.id.tv_price_discount);
        etRemark = findViewById(R.id.et_confirm_remark);

        initTitleBar();
        bindShop();
        buildGoodsList();
        initModeSwitch();
        refreshPrice();

        findViewById(R.id.tv_confirm_submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitOrder();
            }
        });
    }

    private void initTitleBar() {
        ((TextView) findViewById(R.id.tv_title)).setText(R.string.title_confirm_order);
        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /** 门店名、地址、预计出餐时间 */
    private void bindShop() {
        shop = GoodsDataUtils.getShop(this);
        if (shop == null) {
            return;
        }
        ((TextView) findViewById(R.id.tv_confirm_shop)).setText(shop.getName());
        ((TextView) findViewById(R.id.tv_confirm_shop_addr)).setText(shop.getAddress());
        ((TextView) findViewById(R.id.tv_confirm_time))
                .setText("预计 " + shop.getDeliveryTime() + " 出餐");
        ((TextView) findViewById(R.id.tv_confirm_address)).setText(AppPrefs.getAddress(this));
    }

    /** 把购物车每一行渲染成"名称 / 规格 ×数量 小计" */
    private void buildGoodsList() {
        LinearLayout container = findViewById(R.id.ll_confirm_goods);
        container.removeAllViews();
        List<CartItem> items = CartManager.getCartList();
        for (CartItem item : items) {
            View row = LayoutInflater.from(this)
                    .inflate(R.layout.item_order_goods, container, false);
            ((TextView) row.findViewById(R.id.tv_og_name)).setText(item.getName());
            ((TextView) row.findViewById(R.id.tv_og_spec)).setText(item.getSpecText());
            ((TextView) row.findViewById(R.id.tv_og_count)).setText("x" + item.getCount());
            ((TextView) row.findViewById(R.id.tv_og_subtotal))
                    .setText(PriceUtils.format(item.getSubtotal()));
            container.addView(row);
        }
    }

    /** 自取 / 外送切换：只改背景和文字颜色，同时刷新配送费与地址栏 */
    private void initModeSwitch() {
        tvModePickup = findViewById(R.id.tv_mode_pickup);
        tvModeDelivery = findViewById(R.id.tv_mode_delivery);
        llAddress = findViewById(R.id.ll_confirm_address);

        tvModePickup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = OrderBean.MODE_PICKUP;
                applyModeStyle();
                refreshPrice();
            }
        });
        tvModeDelivery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = OrderBean.MODE_DELIVERY;
                applyModeStyle();
                refreshPrice();
            }
        });
        applyModeStyle();
    }

    private void applyModeStyle() {
        boolean pickup = mode == OrderBean.MODE_PICKUP;
        tvModePickup.setBackgroundResource(pickup
                ? R.drawable.bg_chip_selected : R.drawable.bg_chip_normal);
        tvModePickup.setTextColor(getResources().getColor(
                pickup ? R.color.white : R.color.text_gray));

        tvModeDelivery.setBackgroundResource(pickup
                ? R.drawable.bg_chip_normal : R.drawable.bg_chip_selected);
        tvModeDelivery.setTextColor(getResources().getColor(
                pickup ? R.color.text_gray : R.color.white));

        llAddress.setVisibility(pickup ? View.GONE : View.VISIBLE);
    }

    /**
     * 重新算钱。
     * 顺序很重要：满减按"商品小计"判断，不含打包费和配送费（和大多数奶茶店一致）。
     */
    private void refreshPrice() {
        goodsTotal = CartManager.getGoodsTotal();
        packing = PriceUtils.packingFee(CartManager.getTotalCount());
        delivery = PriceUtils.deliveryFee(shop, mode);
        discount = GoodsDataUtils.couponDiscount(this, goodsTotal);
        total = PriceUtils.total(goodsTotal, packing, delivery, discount);

        ((TextView) findViewById(R.id.tv_price_goods)).setText(PriceUtils.format(goodsTotal));
        ((TextView) findViewById(R.id.tv_price_packing)).setText(PriceUtils.format(packing));

        View deliveryRow = findViewById(R.id.ll_price_delivery);
        deliveryRow.setVisibility(mode == OrderBean.MODE_DELIVERY ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.tv_price_delivery)).setText(PriceUtils.format(delivery));

        tvDiscount.setText(discount.compareTo(BigDecimal.ZERO) > 0
                ? "-" + PriceUtils.format(discount) : "未满足");

        tvCoupon.setText(discount.compareTo(BigDecimal.ZERO) > 0
                ? "已减 " + PriceUtils.format(discount)
                : GoodsDataUtils.couponText(this));

        tvTotal.setText(PriceUtils.format(total));
    }

    /**
     * 提交订单：购物车 → OrderBean → 写数据库 → 去收银台。
     * 订单号用"时间戳 + 随机数"，保证同一天多次下单也不会重复。
     */
    private void submitOrder() {
        if (CartManager.isEmpty()) {
            Toast.makeText(this, R.string.cart_empty_tip, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        OrderBean order = new OrderBean();
        order.setOrderNo(buildOrderNo());
        order.setCreatedAt(System.currentTimeMillis());
        order.setGoodsTotal(goodsTotal);
        order.setPackingFee(packing);
        order.setDeliveryFee(delivery);
        order.setDiscount(discount);
        order.setTotal(total);
        order.setStatus(OrderBean.STATUS_UNPAID);
        order.setMode(mode);
        order.setPickupCode(buildPickupCode());
        order.setAddress(mode == OrderBean.MODE_DELIVERY ? AppPrefs.getAddress(this) : "");
        String remark = etRemark == null ? "" : etRemark.getText().toString().trim();
        order.setRemark(remark);

        // 购物车 → 订单明细（保留原规格和算好的单价，方便"再来一单"还原）
        for (CartItem item : CartManager.getCartList()) {
            order.addItem(new OrderItemBean(item.getGoodsId(), item.getName(),
                    item.getSpecText(), item.getUnitPrice(), item.getCount()));
        }

        long id = orderDao.insertOrder(order);
        if (id <= 0) {
            Toast.makeText(this, "下单失败，请重试", Toast.LENGTH_SHORT).show();
            return;
        }

        // 订单已经落库，购物车就可以清空了：
        // 即使这次不付款，订单也会以"待支付"留在订单列表里，可以之后再付。
        CartManager.clear();

        Intent intent = new Intent(this, PayActivity.class);
        intent.putExtra("orderNo", order.getOrderNo());
        startActivity(intent);
        finish();
    }

    private String buildOrderNo() {
        String time = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(new Date());
        int random = new Random().nextInt(900) + 100;
        return "MT" + time + random;
    }

    private String buildPickupCode() {
        return String.valueOf(new Random().nextInt(9000) + 1000);
    }
}
