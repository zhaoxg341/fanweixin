package cn.itcast.order.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.order.R;
import cn.itcast.order.adapter.ShopAdapter;
import cn.itcast.order.bean.ShopBean;
import cn.itcast.order.utils.AppPrefs;
import cn.itcast.order.utils.ShopRepository;
import cn.itcast.order.view.FlowLayout;
import cn.itcast.order.view.ShopListView;

/**
 * 搜索页
 * 支持两种进入方式：
 *   1. 首页搜索框：展示历史 + 热门；
 *   2. 首页分类频道：带 category 参数直接展示该分类的店铺。
 */
public class SearchActivity extends AppCompatActivity {

    private static final String[] HOT_WORDS = {
            "奶茶", "炸鸡", "麻辣烫", "披萨", "水果", "夜宵", "汉堡", "甜品"
    };

    private EditText etSearch;
    private ImageView ivClear;
    private View scrollSuggest;
    private View layoutResult;
    private TextView tvResultCount;
    private TextView tvEmpty;
    private ShopListView lvResult;

    private FlowLayout flowHistory;
    private FlowLayout flowHot;
    private ShopAdapter adapter;

    /** 分类模式：分类关键词与展示名称 */
    private String category = "";
    private String categoryName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        category = getIntent().getStringExtra("category");
        categoryName = getIntent().getStringExtra("categoryName");
        if (category == null) {
            category = "";
        }
        if (categoryName == null) {
            categoryName = "";
        }

        initView();
        initHistoryAndHot();

        if (!categoryName.isEmpty()) {
            // 分类模式：直接展示结果
            etSearch.setHint("搜索" + categoryName + "商家、菜品");
            doSearch("");
        }
    }

    private void initView() {
        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layoutResult.getVisibility() == View.VISIBLE && categoryName.isEmpty()) {
                    showSuggest();
                } else {
                    finish();
                }
            }
        });

        etSearch = findViewById(R.id.et_search);
        ivClear = findViewById(R.id.iv_clear);
        scrollSuggest = findViewById(R.id.scroll_suggest);
        layoutResult = findViewById(R.id.layout_result);
        tvResultCount = findViewById(R.id.tv_result_count);
        tvEmpty = findViewById(R.id.tv_empty);
        lvResult = findViewById(R.id.lv_result);
        flowHistory = findViewById(R.id.flow_history);
        flowHot = findViewById(R.id.flow_hot);
        flowHistory.setSpacing(dp(8), dp(8));
        flowHot.setSpacing(dp(8), dp(8));

        adapter = new ShopAdapter(this);
        lvResult.setAdapter(adapter);
        lvResult.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ShopBean shop = (ShopBean) adapter.getItem(position);
                Intent intent = new Intent(SearchActivity.this, ShopDetailActivity.class);
                intent.putExtra("shop", shop);
                startActivity(intent);
            }
        });

        findViewById(R.id.tv_do_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSearch(etSearch.getText().toString());
            }
        });

        ivClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearch.setText("");
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ivClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    doSearch(etSearch.getText().toString());
                    return true;
                }
                return false;
            }
        });
    }

    private void initHistoryAndHot() {
        flowHistory.removeAllViews();
        flowHot.removeAllViews();

        List<String> history = AppPrefs.getSearchHistory(this);
        if (history.isEmpty()) {
            flowHistory.setVisibility(View.GONE);
        } else {
            flowHistory.setVisibility(View.VISIBLE);
            for (String word : history) {
                flowHistory.addView(createTag(word, false));
            }
        }

        for (String word : HOT_WORDS) {
            flowHot.addView(createTag(word, true));
        }

        findViewById(R.id.tv_clear_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppPrefs.clearSearchHistory(SearchActivity.this);
                flowHistory.removeAllViews();
                flowHistory.setVisibility(View.GONE);
            }
        });
    }

    /** 生成一个可点击的搜索标签 */
    private TextView createTag(final String word, boolean hot) {
        TextView tag = new TextView(this);
        tag.setText(word);
        tag.setTextSize(12);
        tag.setTextColor(getResources().getColor(hot ? R.color.orange : R.color.text_gray));
        tag.setBackgroundResource(hot ? R.drawable.bg_tag_orange : R.drawable.bg_tag_gray);
        tag.setPadding(dp(12), dp(6), dp(12), dp(6));
        tag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearch.setText(word);
                etSearch.setSelection(word.length());
                doSearch(word);
            }
        });
        return tag;
    }

    /** 执行搜索：分类过滤 + 关键词过滤 */
    private void doSearch(String keyword) {
        String key = keyword == null ? "" : keyword.trim();
        List<ShopBean> source;
        if (category.isEmpty() || "全部".equals(category)) {
            source = ShopRepository.getAll(this);
        } else {
            source = ShopRepository.byCategory(this, category);
        }

        List<ShopBean> result = new ArrayList<>();
        for (ShopBean shop : source) {
            if (key.isEmpty() || matchKeyword(shop, key)) {
                result.add(shop);
            }
        }

        if (!key.isEmpty()) {
            AppPrefs.addSearchHistory(this, key);
        }

        adapter.setData(result);
        String title = categoryName.isEmpty() ? "" : categoryName + " · ";
        tvResultCount.setText(title + "找到 " + result.size() + " 家店铺");
        tvEmpty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
        lvResult.setVisibility(result.isEmpty() ? View.GONE : View.VISIBLE);

        scrollSuggest.setVisibility(View.GONE);
        layoutResult.setVisibility(View.VISIBLE);

        hideKeyboard();
    }

    private boolean matchKeyword(ShopBean shop, String key) {
        String lower = key.toLowerCase();
        if (shop.getShopName() != null && shop.getShopName().toLowerCase().contains(lower)) {
            return true;
        }
        if (shop.getCategory() != null && shop.getCategory().toLowerCase().contains(lower)) {
            return true;
        }
        for (cn.itcast.order.bean.FoodBean food : shop.getFoodList()) {
            if (food.getFoodName() != null && food.getFoodName().toLowerCase().contains(lower)) {
                return true;
            }
        }
        return false;
    }

    private void showSuggest() {
        layoutResult.setVisibility(View.GONE);
        scrollSuggest.setVisibility(View.VISIBLE);
        etSearch.setText("");
        initHistoryAndHot();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        if (layoutResult.getVisibility() == View.VISIBLE && categoryName.isEmpty()) {
            showSuggest();
            return;
        }
        super.onBackPressed();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
