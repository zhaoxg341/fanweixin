package cn.itcast.order.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cn.itcast.order.R;
import cn.itcast.order.bean.FoodBean;
import cn.itcast.order.utils.CartManager;
import cn.itcast.order.utils.ShopDataUtils;

/**
 * 菜品详情界面（对应教材 12.6 菜品详情模块）
 *
 * 菜品对象通过 Intent 传递（FoodBean 实现了 Serializable），
 * 点击“加入购物车”直接累加到 CartManager，返回店铺详情页时数量会自动同步。
 */
public class FoodDetailActivity extends AppCompatActivity {

    private FoodBean food;

    private TextView tvBarPrice;
    private TextView tvBarCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_detail);

        food = (FoodBean) getIntent().getSerializableExtra("food");
        if (food == null) {
            Toast.makeText(this, "菜品信息获取失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initView();
        refreshBar();
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
        tvTitle.setText(food.getFoodName());

        // 菜品大图
        ImageView ivPic = findViewById(R.id.iv_detail_food_pic);
        int resId = ShopDataUtils.getDrawableId(this, food.getFoodPic());
        if (resId != 0) {
            ivPic.setImageResource(resId);
        }

        TextView tvName = findViewById(R.id.tv_detail_food_name);
        tvName.setText(food.getFoodName());
        TextView tvPopularity = findViewById(R.id.tv_detail_popularity);
        tvPopularity.setText(food.getPopularity());
        TextView tvSale = findViewById(R.id.tv_detail_sale);
        tvSale.setText(String.format("月售%d", food.getSaleNum()));
        TextView tvPrice = findViewById(R.id.tv_detail_price);
        tvPrice.setText(food.getPriceString());

        // 底部操作栏
        tvBarPrice = findViewById(R.id.tv_bar_price);
        tvBarCount = findViewById(R.id.tv_bar_count);
        tvBarPrice.setText(food.getPriceString());

        findViewById(R.id.tv_add_cart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CartManager.add(food);
                refreshBar();
                Toast.makeText(FoodDetailActivity.this,
                        "已加入购物车：" + food.getFoodName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** 刷新底部栏：单价 + 当前购物车中该菜品的数量 */
    private void refreshBar() {
        int count = CartManager.getCount(food.getFoodId());
        tvBarPrice.setText(food.getPriceString());
        if (count > 0) {
            tvBarCount.setText("购物车中已有 " + count + " 份");
        } else {
            tvBarCount.setText("购物车中还没有这道菜");
        }
    }
}
