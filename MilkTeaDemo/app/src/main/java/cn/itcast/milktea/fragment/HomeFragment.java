package cn.itcast.milktea.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.math.BigDecimal;
import java.util.List;

import cn.itcast.milktea.R;
import cn.itcast.milktea.activity.CartActivity;
import cn.itcast.milktea.activity.GoodsDetailActivity;
import cn.itcast.milktea.activity.SearchActivity;
import cn.itcast.milktea.adapter.GoodsAdapter;
import cn.itcast.milktea.bean.CategoryBean;
import cn.itcast.milktea.bean.GoodsBean;
import cn.itcast.milktea.bean.ShopBean;
import cn.itcast.milktea.utils.CartManager;
import cn.itcast.milktea.utils.GoodsDataUtils;
import cn.itcast.milktea.utils.PriceUtils;

/**
 * 点单首页。
 *
 * 页面结构：渐变头部（店名 + 搜索 + banner）→ 公告 → 分类横滑栏 → 商品列表 → 底部购物车栏。
 * 分类切换不重新请求数据，直接从内存里按 categoryId 过滤，
 * 因为商品总共就十几条，过滤比"重新加载"快得多。
 */
public class HomeFragment extends Fragment implements GoodsAdapter.OnGoodsActionListener {

    private TextView tvShopName;
    private ImageView ivBanner;
    private TextView tvNotice;
    private LinearLayout llCategories;
    private ListView lvGoods;
    private TextView tvCartBadge;
    private TextView tvCartTotal;
    private TextView tvCartTip;
    private TextView tvGoSettle;

    private GoodsAdapter adapter;
    private List<CategoryBean> categories;
    private int currentCategoryId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initView(view);
        initData();
        return view;
    }

    private void initView(View view) {
        tvShopName = view.findViewById(R.id.tv_shop_name);
        ivBanner = view.findViewById(R.id.iv_banner);
        tvNotice = view.findViewById(R.id.tv_notice);
        llCategories = view.findViewById(R.id.ll_categories);
        lvGoods = view.findViewById(R.id.lv_goods);
        tvCartBadge = view.findViewById(R.id.tv_cart_badge);
        tvCartTotal = view.findViewById(R.id.tv_cart_total);
        tvCartTip = view.findViewById(R.id.tv_cart_tip);
        tvGoSettle = view.findViewById(R.id.tv_go_settle);

        // banner 圆角：背景给个圆角形状，再裁剪内容
        ivBanner.setBackgroundResource(R.drawable.bg_card_white);
        ivBanner.setClipToOutline(true);

        adapter = new GoodsAdapter(getActivity(), this);
        lvGoods.setAdapter(adapter);

        view.findViewById(R.id.ll_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), SearchActivity.class));
            }
        });
        view.findViewById(R.id.iv_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), SearchActivity.class));
            }
        });
        view.findViewById(R.id.iv_cart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCart();
            }
        });
        tvGoSettle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCart();
            }
        });
    }

    private void initData() {
        if (getActivity() == null) {
            return;
        }
        ShopBean shop = GoodsDataUtils.getShop(getActivity());
        if (shop != null) {
            tvShopName.setText(shop.getName());
            tvNotice.setText(shop.getNotice() + " · " + shop.getOpenTime());
            int bannerRes = GoodsDataUtils.getDrawableId(getActivity(), shop.getBanner());
            if (bannerRes != 0) {
                ivBanner.setImageResource(bannerRes);
            }
        }

        categories = GoodsDataUtils.getCategories(getActivity());
        buildCategoryBar();
        if (!categories.isEmpty()) {
            selectCategory(categories.get(0).getId());
        }
    }

    /** 动态生成分类 chip（HorizontalScrollView 里没法直接放 ListView） */
    private void buildCategoryBar() {
        llCategories.removeAllViews();
        for (final CategoryBean category : categories) {
            TextView chip = (TextView) LayoutInflater.from(getActivity())
                    .inflate(R.layout.item_chip, llCategories, false);
            chip.setText(category.getName());
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectCategory(category.getId());
                }
            });
            llCategories.addView(chip);
        }
    }

    /** 切换分类：更新所有 chip 的选中态 + 过滤商品 */
    private void selectCategory(int categoryId) {
        currentCategoryId = categoryId;
        for (int i = 0; i < llCategories.getChildCount(); i++) {
            TextView chip = (TextView) llCategories.getChildAt(i);
            boolean selected = categories.get(i).getId() == categoryId;
            chip.setSelected(selected);
            chip.setTextColor(getResources().getColor(
                    selected ? R.color.white : R.color.text_black));
        }
        List<GoodsBean> goods = GoodsDataUtils.getGoodsByCategory(getActivity(), categoryId);
        adapter.setData(goods);
    }

    /** 商品卡片点击 → 详情页 */
    @Override
    public void onGoodsClick(GoodsBean goods) {
        Intent intent = new Intent(getActivity(), GoodsDetailActivity.class);
        intent.putExtra("goodsId", goods.getGoodsId());
        startActivity(intent);
    }

    /** 列表里的加号/减号改变了购物车 */
    @Override
    public void onCartChanged() {
        refreshCartBar();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 从详情页 / 购物车返回，数量可能变了，重画一遍
        adapter.notifyDataSetChanged();
        refreshCartBar();
    }

    /** 刷新底部购物车栏 */
    private void refreshCartBar() {
        if (getActivity() == null) {
            return;
        }
        int count = CartManager.getTotalCount();
        BigDecimal total = CartManager.getGoodsTotal();

        if (count > 0) {
            tvCartBadge.setVisibility(View.VISIBLE);
            tvCartBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            tvCartBadge.setVisibility(View.GONE);
        }
        tvCartTotal.setText(PriceUtils.format(total));

        if (count == 0) {
            tvCartTip.setText(GoodsDataUtils.couponText(getActivity()));
            tvGoSettle.setBackgroundResource(R.drawable.bg_btn_gray);
        } else if (GoodsDataUtils.couponDiscount(getActivity(), total)
                .compareTo(BigDecimal.ZERO) > 0) {
            tvCartTip.setText("已满减 "
                    + PriceUtils.format(GoodsDataUtils.couponDiscount(getActivity(), total))
                    + "，结算时自动抵扣");
            tvGoSettle.setBackgroundResource(R.drawable.bg_btn_primary_selector);
        } else {
            tvCartTip.setText(GoodsDataUtils.nextCouponTip(getActivity(), total));
            tvGoSettle.setBackgroundResource(R.drawable.bg_btn_primary_selector);
        }
    }

    private void openCart() {
        if (CartManager.isEmpty()) {
            Toast.makeText(getActivity(), R.string.cart_empty_tip, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(getActivity(), CartActivity.class));
    }
}
