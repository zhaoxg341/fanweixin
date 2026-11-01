package cn.itcast.milktea.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import cn.itcast.milktea.R;
import cn.itcast.milktea.adapter.GoodsAdapter;
import cn.itcast.milktea.bean.GoodsBean;
import cn.itcast.milktea.utils.GoodsDataUtils;

/**
 * 搜索页：按名称/描述模糊匹配商品。
 *
 * 搜索不用另写适配器 —— 直接复用首页的 GoodsAdapter，
 * 它内部处理了图片、划线价、购物车加减号，搜索页只要把结果列表塞进去就行。
 */
public class SearchActivity extends AppCompatActivity implements GoodsAdapter.OnGoodsActionListener {

    private EditText etSearch;
    private ListView lvSearch;
    private View emptyView;
    private GoodsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etSearch = findViewById(R.id.et_search);
        lvSearch = findViewById(R.id.lv_search);
        emptyView = findViewById(R.id.tv_search_empty);

        adapter = new GoodsAdapter(this, this);
        lvSearch.setAdapter(adapter);

        findViewById(R.id.iv_search_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.tv_search_do).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSearch();
            }
        });

        // 软键盘上的"搜索"键
        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    doSearch();
                    return true;
                }
                return false;
            }
        });

        // 边输边搜：清空输入框就恢复全部商品
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    showAll();
                }
            }
        });

        showAll();
    }

    /** 一进来先展示全部商品，比一片空白友好 */
    private void showAll() {
        adapter.setData(GoodsDataUtils.getGoods(this));
        emptyView.setVisibility(View.GONE);
        lvSearch.setVisibility(View.VISIBLE);
    }

    private void doSearch() {
        String keyword = etSearch.getText().toString().trim();
        if (keyword.isEmpty()) {
            showAll();
            return;
        }
        List<GoodsBean> result = GoodsDataUtils.search(this, keyword);
        adapter.setData(result);
        boolean empty = result.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        lvSearch.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onGoodsClick(GoodsBean goods) {
        Intent intent = new Intent(this, GoodsDetailActivity.class);
        intent.putExtra("goodsId", goods.getGoodsId());
        startActivity(intent);
    }

    @Override
    public void onCartChanged() {
        // 搜索页没有底部购物车栏，不需要额外刷新
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从详情页可能加了购物车，回来要让数量角标更新
        adapter.notifyDataSetChanged();
    }
}
