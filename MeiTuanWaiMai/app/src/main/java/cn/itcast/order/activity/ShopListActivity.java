package cn.itcast.order.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cn.itcast.order.R;
import cn.itcast.order.adapter.AdBannerAdapter;
import cn.itcast.order.adapter.ShopAdapter;
import cn.itcast.order.bean.ShopBean;
import cn.itcast.order.utils.ShopRepository;
import cn.itcast.order.view.ShopListView;
import cn.itcast.order.view.ViewPagerIndicator;

/**
 * 首页：店铺列表
 *
 * 相比教材版本新增了：
 *   1. 顶部定位 + 搜索入口；
 *   2. 分类频道（美食/快餐/甜品...）；
 *   3. 排序（综合/销量/评分/起送价）与快捷筛选（满减/免配送费）；
 *   4. 底部导航（首页/订单/我的）。
 */
public class ShopListActivity extends AppCompatActivity {

    /** 广告自动轮播间隔 */
    private static final long SCROLL_INTERVAL = 3000L;

    private static final int SORT_DEFAULT = 0;
    private static final int SORT_SALE = 1;
    private static final int SORT_SCORE = 2;
    private static final int SORT_OFFER = 3;

    private ViewPager vpBanner;
    private ViewPagerIndicator indicator;
    private ShopListView lvShop;
    private TextView tvNearbyTitle;

    private AdBannerAdapter bannerAdapter;
    private ShopAdapter shopAdapter;

    /** 全量数据与当前展示数据分开，筛选排序不影响原始顺序 */
    private final List<ShopBean> allShops = new ArrayList<>();
    private final List<ShopBean> displayShops = new ArrayList<>();

    private int sortType = SORT_DEFAULT;
    private boolean filterDiscount = false;
    private boolean filterFree = false;

    private TextView[] sortViews;
    private TextView tvFilterDiscount;
    private TextView tvFilterFree;

    /** 主线程 Handler：负责广告栏自动轮播 */
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean autoScroll = true;

    private final Runnable scrollTask = new Runnable() {
        @Override
        public void run() {
            if (!autoScroll || bannerAdapter == null || bannerAdapter.getCount() == 0) {
                return;
            }
            int next = (vpBanner.getCurrentItem() + 1) % bannerAdapter.getCount();
            vpBanner.setCurrentItem(next, true);
            handler.postDelayed(this, SCROLL_INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_list);
        initView();
        initBottomNav();
        initData();
    }

    private void initView() {
        vpBanner = findViewById(R.id.vp_banner);
        indicator = findViewById(R.id.indicator);
        lvShop = findViewById(R.id.lv_shop);
        tvNearbyTitle = findViewById(R.id.tv_nearby_title);

        // 搜索
        findViewById(R.id.layout_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSearch("", "");
            }
        });
        findViewById(R.id.tv_search_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSearch("", "");
            }
        });

        // 分类频道
        bindCategory(R.id.cat_food, "全部", "美食");
        bindCategory(R.id.cat_fast, "快餐|汉堡|小吃|面食", "快餐");
        bindCategory(R.id.cat_dessert, "甜品", "甜品");
        bindCategory(R.id.cat_tea, "奶茶|饮品", "奶茶");
        bindCategory(R.id.cat_fruit, "水果", "水果");
        bindCategory(R.id.cat_market, "超市", "超市");
        bindCategory(R.id.cat_drug, "药品", "药品");
        bindCategory(R.id.cat_flower, "鲜花", "鲜花");

        // 排序
        TextView tvSortDefault = findViewById(R.id.tv_sort_default);
        TextView tvSortSale = findViewById(R.id.tv_sort_sale);
        TextView tvSortScore = findViewById(R.id.tv_sort_score);
        TextView tvSortOffer = findViewById(R.id.tv_sort_offer);
        sortViews = new TextView[]{tvSortDefault, tvSortSale, tvSortScore, tvSortOffer};
        for (int i = 0; i < sortViews.length; i++) {
            final int type = i;
            sortViews[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sortType = type;
                    updateSortState();
                    applyFilter();
                }
            });
        }

        // 快捷筛选
        tvFilterDiscount = findViewById(R.id.tv_filter_discount);
        tvFilterFree = findViewById(R.id.tv_filter_free);
        tvFilterDiscount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterDiscount = !filterDiscount;
                updateFilterState();
                applyFilter();
            }
        });
        tvFilterFree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterFree = !filterFree;
                updateFilterState();
                applyFilter();
            }
        });

        // 广告栏
        bannerAdapter = new AdBannerAdapter(this);
        vpBanner.setAdapter(bannerAdapter);
        vpBanner.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                indicator.setCurrentPosition(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        // 店铺列表
        shopAdapter = new ShopAdapter(this);
        lvShop.setAdapter(shopAdapter);
        lvShop.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ShopBean shop = (ShopBean) shopAdapter.getItem(position);
                Intent intent = new Intent(ShopListActivity.this, ShopDetailActivity.class);
                intent.putExtra("shop", shop);
                startActivity(intent);
            }
        });

        // 初始化排序 / 筛选的选中状态
        updateSortState();
        updateFilterState();
    }

    /** 分类入口：点击带上分类关键词跳到搜索结果页 */
    private void bindCategory(int viewId, final String categories, final String title) {
        findViewById(viewId).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSearch(categories, title);
            }
        });
    }

    private void openSearch(String category, String title) {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra("category", category);
        intent.putExtra("categoryName", title);
        startActivity(intent);
    }

    private void initBottomNav() {
        View navHome = findViewById(R.id.nav_home);
        View navOrder = findViewById(R.id.nav_order);
        View navMine = findViewById(R.id.nav_mine);
        navHome.setSelected(true);
        navOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ShopListActivity.this, OrderListActivity.class));
            }
        });
        navMine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ShopListActivity.this, MineActivity.class));
            }
        });
    }

    /**
     * 数据加载放到子线程，解析完再回主线程刷新
     */
    private void initData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<ShopBean> shops = new ArrayList<>(ShopRepository.getAll(ShopListActivity.this));
                final List<String> banners = new ArrayList<>(ShopRepository.getBanners(ShopListActivity.this));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        allShops.clear();
                        allShops.addAll(shops);

                        bannerAdapter.setData(banners);
                        indicator.setCount(banners.size());
                        indicator.setCurrentPosition(0);

                        applyFilter();
                        if (shops.isEmpty()) {
                            Toast.makeText(ShopListActivity.this,
                                    "数据加载失败，请检查 assets/shop.json", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }

    /** 按当前筛选条件 + 排序方式刷新列表 */
    private void applyFilter() {
        displayShops.clear();
        for (ShopBean shop : allShops) {
            if (filterDiscount && !hasDiscount(shop)) {
                continue;
            }
            if (filterFree && shop.getDistributionCost() != null
                    && shop.getDistributionCost().doubleValue() > 0) {
                continue;
            }
            displayShops.add(shop);
        }
        sort(displayShops);
        shopAdapter.setData(displayShops);
        tvNearbyTitle.setText(displayShops.isEmpty()
                ? "附近商家（没有符合条件的店铺）"
                : "附近商家（" + displayShops.size() + "）");
    }

    private boolean hasDiscount(ShopBean shop) {
        String feature = shop.getFeature();
        return feature != null && feature.contains("减");
    }

    private void sort(List<ShopBean> shops) {
        switch (sortType) {
            case SORT_SALE:
                Collections.sort(shops, new Comparator<ShopBean>() {
                    @Override
                    public int compare(ShopBean o1, ShopBean o2) {
                        return o2.getSaleNum() - o1.getSaleNum();
                    }
                });
                break;
            case SORT_SCORE:
                Collections.sort(shops, new Comparator<ShopBean>() {
                    @Override
                    public int compare(ShopBean o1, ShopBean o2) {
                        return Float.compare(o2.getScore(), o1.getScore());
                    }
                });
                break;
            case SORT_OFFER:
                Collections.sort(shops, new Comparator<ShopBean>() {
                    @Override
                    public int compare(ShopBean o1, ShopBean o2) {
                        return compareMoney(o1, o2);
                    }
                });
                break;
            default:
                // 综合排序：保持 json 里的原始顺序
                break;
        }
    }

    private int compareMoney(ShopBean o1, ShopBean o2) {
        double v1 = o1.getOfferPrice() == null ? 0 : o1.getOfferPrice().doubleValue();
        double v2 = o2.getOfferPrice() == null ? 0 : o2.getOfferPrice().doubleValue();
        return Double.compare(v1, v2);
    }

    private void updateSortState() {
        for (int i = 0; i < sortViews.length; i++) {
            sortViews[i].setSelected(i == sortType);
        }
    }

    private void updateFilterState() {
        styleFilter(tvFilterDiscount, filterDiscount);
        styleFilter(tvFilterFree, filterFree);
    }

    private void styleFilter(TextView view, boolean active) {
        view.setSelected(active);
        view.setBackgroundResource(active ? R.drawable.bg_tag_orange : R.drawable.bg_tag_gray);
        view.setTextColor(getResources().getColor(active ? R.color.orange : R.color.text_gray));
    }

    @Override
    protected void onResume() {
        super.onResume();
        autoScroll = true;
        handler.removeCallbacks(scrollTask);
        handler.postDelayed(scrollTask, SCROLL_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        autoScroll = false;
        handler.removeCallbacks(scrollTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
