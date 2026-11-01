package cn.itcast.order.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;
import java.math.RoundingMode;

import cn.itcast.order.R;
import cn.itcast.order.adapter.CartAdapter;
import cn.itcast.order.adapter.MenuAdapter;
import cn.itcast.order.bean.FoodBean;
import cn.itcast.order.bean.ShopBean;
import cn.itcast.order.utils.AppPrefs;
import cn.itcast.order.utils.CartManager;
import cn.itcast.order.utils.ShopDataUtils;

/**
 * 店铺详情界面（对应教材 12.5 店铺详情与购物车模块）
 *
 * 页面结构：标题栏 + 店铺信息 + 菜单列表 + 底部购物车栏
 * 交互要点：
 *   1. 点击“+”/“-”直接操作 CartManager，适配器只负责展示；
 *   2. 点击购物车图标弹出购物车窗口（PopupWindow）；
 *   3. 点击“去结算”在满足起送价后进入订单确认页。
 */
public class ShopDetailActivity extends AppCompatActivity implements MenuAdapter.OnMenuListener {

    /** 底部购物车栏高度（与布局中的 56dp 保持一致） */
    private static final int BOTTOM_BAR_HEIGHT_DP = 56;
    /** 购物车弹窗中“已选商品”标题栏的高度 */
    private static final int POPUP_HEADER_HEIGHT_DP = 44;
    /** 购物车弹窗中每个条目的高度（与 cart_item.xml 的 56dp 一致） */
    private static final int POPUP_ITEM_HEIGHT_DP = 56;
    /** 购物车弹窗最大高度 */
    private static final int POPUP_MAX_HEIGHT_DP = 300;

    private ShopBean shop;

    private MenuAdapter menuAdapter;
    private CartAdapter cartAdapter;

    private TextView tvCartBadge;
    private TextView tvCartTotal;
    private TextView tvCartTip;
    private TextView tvSettle;

    private PopupWindow cartPopup;
    private FrameLayout popupContainer;
    private View popupContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_detail);

        shop = (ShopBean) getIntent().getSerializableExtra("shop");
        if (shop == null) {
            Toast.makeText(this, "店铺信息获取失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 切换店铺时清空上一次未结算的购物车
        CartManager.switchShop(shop.getId());

        initView();
        initCartPopup();
        refreshCartBar();
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
        tvTitle.setText(shop.getShopName());

        // 标题栏右侧：收藏 / 已收藏
        final TextView tvFavorite = findViewById(R.id.tv_right);
        updateFavoriteState(tvFavorite);
        tvFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean favorite = AppPrefs.toggleFavorite(ShopDetailActivity.this, shop.getId());
                updateFavoriteState(tvFavorite);
                Toast.makeText(ShopDetailActivity.this,
                        favorite ? "已收藏，可在“我的-收藏”中查看" : "已取消收藏",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // 评价入口
        TextView tvReviewSummary = findViewById(R.id.tv_review_summary);
        tvReviewSummary.setText(String.format("%.1f分 · %d条评价",
                shop.getScore(), 128 + shop.getId() * 7));
        findViewById(R.id.layout_review_entry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ShopDetailActivity.this, ReviewListActivity.class);
                intent.putExtra("shop", shop);
                startActivity(intent);
            }
        });

        // 店铺信息区
        ImageView ivLogo = findViewById(R.id.iv_shop_logo);
        int logoRes = ShopDataUtils.getDrawableId(this, shop.getBanner());
        if (logoRes != 0) {
            ivLogo.setImageResource(logoRes);
        }
        TextView tvShopName = findViewById(R.id.tv_detail_shop_name);
        tvShopName.setText(shop.getShopName());
        TextView tvScore = findViewById(R.id.tv_detail_score);
        tvScore.setText(String.format("★ %.1f  月售%d  %s",
                shop.getScore(), shop.getSaleNum(), shop.getTime()));
        TextView tvOffer = findViewById(R.id.tv_detail_offer);
        tvOffer.setText(shop.getOfferText() + " · " + shop.getDistributionText());
        TextView tvFeature = findViewById(R.id.tv_detail_feature);
        tvFeature.setText(shop.getFeature());

        // 菜单标题
        TextView tvMenuHeader = findViewById(R.id.tv_menu_header);
        tvMenuHeader.setText("菜单（共 " + shop.getFoodList().size() + " 道菜）");

        // 菜单列表
        ListView lvMenu = findViewById(R.id.lv_menu);
        menuAdapter = new MenuAdapter(this, this);
        lvMenu.setAdapter(menuAdapter);
        menuAdapter.setData(shop.getFoodList());

        // 底部购物车栏
        tvCartBadge = findViewById(R.id.tv_cart_badge);
        tvCartTotal = findViewById(R.id.tv_cart_total);
        tvCartTip = findViewById(R.id.tv_cart_tip);
        tvSettle = findViewById(R.id.tv_settle);

        ImageView ivCart = findViewById(R.id.iv_cart);
        ivCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CartManager.isEmpty()) {
                    Toast.makeText(ShopDetailActivity.this, "购物车还是空的，先选几道菜吧",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (cartPopup.isShowing()) {
                    cartPopup.dismiss();
                } else {
                    showCartPopup();
                }
            }
        });

        tvSettle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToOrder();
            }
        });
    }

    /** 创建购物车弹窗（结构：半透明遮罩 + 底部购物车列表） */
    private void initCartPopup() {
        popupContainer = new FrameLayout(this);

        // 1. 半透明遮罩，点击空白处关闭弹窗
        View dim = new View(this);
        dim.setBackgroundColor(0x99000000);
        dim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cartPopup.dismiss();
            }
        });
        popupContainer.addView(dim, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // 2. 购物车内容，靠屏幕底部显示
        popupContentView = LayoutInflater.from(this).inflate(R.layout.cart_popup, popupContainer, false);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, popupHeight());
        contentParams.gravity = Gravity.BOTTOM;
        popupContainer.addView(popupContentView, contentParams);

        // 购物车列表
        ListView lvCart = popupContentView.findViewById(R.id.lv_cart);
        cartAdapter = new CartAdapter(this, new CartAdapter.OnCartChangedListener() {
            @Override
            public void onCartChanged() {
                // 弹窗里加减数量：菜单列表与底部栏一起刷新
                menuAdapter.notifyDataSetChanged();
                refreshCartBar();
                if (CartManager.isEmpty()) {
                    cartPopup.dismiss();
                } else {
                    cartAdapter.setData(CartManager.getCartList());
                    resizePopup();
                }
            }
        });
        lvCart.setAdapter(cartAdapter);

        // 清空购物车
        popupContentView.findViewById(R.id.tv_clear_cart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearCartDialog();
            }
        });

        cartPopup = new PopupWindow(popupContainer,
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        cartPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        cartPopup.setOutsideTouchable(true);
        cartPopup.setFocusable(true);
    }

    /** 显示购物车弹窗：向上偏移底部购物车栏的高度，保持底部栏可见 */
    private void showCartPopup() {
        cartAdapter.setData(CartManager.getCartList());
        resizePopup();
        cartPopup.showAtLocation(findViewById(R.id.iv_cart), Gravity.BOTTOM,
                0, dp(BOTTOM_BAR_HEIGHT_DP));
    }

    /** 根据购物车条目数量重新计算弹窗高度 */
    private void resizePopup() {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) popupContentView.getLayoutParams();
        if (params != null) {
            params.height = popupHeight();
            popupContentView.setLayoutParams(params);
        }
    }

    /** 弹窗高度 = 标题栏 + 条目高度 × 数量，最高不超过 300dp */
    private int popupHeight() {
        int itemCount = Math.max(CartManager.getCartList().size(), 1);
        int height = POPUP_HEADER_HEIGHT_DP + POPUP_ITEM_HEIGHT_DP * itemCount;
        return dp(Math.min(height, POPUP_MAX_HEIGHT_DP));
    }

    /** 确认清空购物车对话框（对应教材 12.5.4） */
    private void showClearCartDialog() {
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
                menuAdapter.notifyDataSetChanged();
                refreshCartBar();
                dialog.dismiss();
                cartPopup.dismiss();
            }
        });
        dialog.show();
    }

    /** 刷新底部购物车栏：数量角标、总价、起送提示、结算按钮状态 */
    private void refreshCartBar() {
        int totalCount = CartManager.getTotalCount();
        BigDecimal totalPrice = CartManager.getTotalPrice();

        if (totalCount > 0) {
            tvCartBadge.setVisibility(View.VISIBLE);
            tvCartBadge.setText(totalCount > 99 ? "99+" : String.valueOf(totalCount));
        } else {
            tvCartBadge.setVisibility(View.GONE);
        }

        tvCartTotal.setText("¥" + totalPrice.setScale(2, RoundingMode.HALF_UP).toPlainString());

        BigDecimal offerPrice = offerPrice();
        if (totalCount == 0) {
            tvCartTip.setText(shop.getOfferText());
            setSettleEnabled(false);
        } else if (totalPrice.compareTo(offerPrice) >= 0) {
            tvCartTip.setText("已满足起送价，可以结算了");
            setSettleEnabled(true);
        } else {
            BigDecimal diff = offerPrice.subtract(totalPrice).setScale(2, RoundingMode.HALF_UP);
            tvCartTip.setText("还差 ¥" + diff.toPlainString() + " 起送");
            setSettleEnabled(false);
        }
    }

    /** 切换“去结算”按钮的可用状态（灰色 / 橙色） */
    private void setSettleEnabled(boolean enabled) {
        tvSettle.setEnabled(enabled);
        tvSettle.setBackgroundResource(enabled
                ? R.drawable.bg_btn_orange_selector : R.drawable.bg_btn_gray);
    }

    /** 进入订单确认页 */
    private void goToOrder() {
        if (CartManager.getTotalPrice().compareTo(offerPrice()) < 0) {
            Toast.makeText(this, "还没达到起送价哦", Toast.LENGTH_SHORT).show();
            return;
        }
        if (cartPopup.isShowing()) {
            cartPopup.dismiss();
        }
        Intent intent = new Intent(this, OrderActivity.class);
        intent.putExtra("shop", shop);
        startActivity(intent);
    }

    private BigDecimal offerPrice() {
        return shop.getOfferPrice() == null ? BigDecimal.ZERO : shop.getOfferPrice();
    }

    /** 刷新收藏按钮文案 */
    private void updateFavoriteState(TextView view) {
        boolean favorite = AppPrefs.isFavorite(this, shop.getId());
        view.setText(favorite ? "已收藏" : "收藏");
    }

    // ---------------- MenuAdapter 回调 ----------------

    @Override
    public void onFoodClick(FoodBean food) {
        Intent intent = new Intent(this, FoodDetailActivity.class);
        intent.putExtra("food", food);
        startActivity(intent);
    }

    @Override
    public void onCartChanged() {
        refreshCartBar();
        if (cartPopup.isShowing()) {
            if (CartManager.isEmpty()) {
                cartPopup.dismiss();
            } else {
                cartAdapter.setData(CartManager.getCartList());
                resizePopup();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从菜品详情页返回时同步最新数量
        menuAdapter.notifyDataSetChanged();
        cartAdapter.setData(CartManager.getCartList());
        refreshCartBar();
    }

    @Override
    public void onBackPressed() {
        if (cartPopup != null && cartPopup.isShowing()) {
            cartPopup.dismiss();
            return;
        }
        super.onBackPressed();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
