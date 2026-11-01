package cn.itcast.order.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import cn.itcast.order.R;
import cn.itcast.order.adapter.ReviewAdapter;
import cn.itcast.order.bean.ShopBean;
import cn.itcast.order.utils.ReviewDataUtils;

/**
 * 店铺评价列表
 */
public class ReviewListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_list);

        ShopBean shop = (ShopBean) getIntent().getSerializableExtra("shop");

        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText("用户评价");

        TextView tvBigScore = findViewById(R.id.tv_big_score);
        TextView tvCount = findViewById(R.id.tv_review_count);
        ListView lvReview = findViewById(R.id.lv_review);
        ReviewAdapter adapter = new ReviewAdapter(this);
        lvReview.setAdapter(adapter);

        if (shop != null) {
            int count = 128 + shop.getId() * 7;
            tvBigScore.setText(String.format("%.1f", shop.getScore()));
            tvCount.setText("共 " + count + " 条评价");
            adapter.setData(ReviewDataUtils.build(shop));
        }
        tvTitle.setText(shop == null ? "用户评价" : shop.getShopName() + " · 评价");
    }
}
