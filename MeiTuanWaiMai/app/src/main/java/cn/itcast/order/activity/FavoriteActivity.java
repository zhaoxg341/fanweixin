package cn.itcast.order.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import cn.itcast.order.R;
import cn.itcast.order.adapter.ShopAdapter;
import cn.itcast.order.bean.ShopBean;
import cn.itcast.order.utils.ShopRepository;
import cn.itcast.order.view.ShopListView;

/**
 * 我的收藏
 */
public class FavoriteActivity extends AppCompatActivity {

    private ShopAdapter adapter;
    private ShopListView lvFavorite;
    private View layoutEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText("我的收藏");

        lvFavorite = findViewById(R.id.lv_favorite);
        layoutEmpty = findViewById(R.id.layout_empty);

        adapter = new ShopAdapter(this);
        lvFavorite.setAdapter(adapter);
        lvFavorite.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ShopBean shop = (ShopBean) adapter.getItem(position);
                Intent intent = new Intent(FavoriteActivity.this, ShopDetailActivity.class);
                intent.putExtra("shop", shop);
                startActivity(intent);
            }
        });

        findViewById(R.id.tv_find_shop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FavoriteActivity.this, ShopListActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<ShopBean> list = ShopRepository.getFavorites(this);
        adapter.setData(list);
        boolean empty = list.isEmpty();
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        lvFavorite.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
