package cn.itcast.milktea.activity;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import cn.itcast.milktea.R;
import cn.itcast.milktea.bean.GoodsBean;
import cn.itcast.milktea.bean.OptionBean;
import cn.itcast.milktea.utils.CartManager;
import cn.itcast.milktea.utils.GoodsDataUtils;
import cn.itcast.milktea.utils.PriceUtils;

/**
 * 商品详情页：选杯型 / 温度 / 糖度 / 加料，实时算价，加入购物车。
 *
 * 单价 = 商品基础价 + 杯型加价 + 所有加料加价；
 * 规格文本会一起存进购物车，比如 "大杯/少冰/半糖/加珍珠、椰果"。
 */
public class GoodsDetailActivity extends AppCompatActivity {

    private GoodsBean goods;

    private OptionBean selectedSize;
    private String selectedTemp = "";
    private String selectedSugar = "";
    private final List<OptionBean> selectedToppings = new ArrayList<>();
    private int quantity = 1;

    private TextView tvPrice;
    private TextView tvQuantity;
    private TextView tvAddCart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goods_detail);

        int goodsId = getIntent().getIntExtra("goodsId", -1);
        goods = GoodsDataUtils.findGoods(this, goodsId);
        if (goods == null) {
            Toast.makeText(this, "商品不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initTitleBar();
        bindGoods();
        initSpecs();
        initBottomBar();
        updatePrice();
    }

    private void initTitleBar() {
        TextView title = findViewById(R.id.tv_title);
        title.setText(goods.getName());
        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void bindGoods() {
        ImageView ivPic = findViewById(R.id.iv_goods_pic);
        int resId = GoodsDataUtils.getDrawableId(this, goods.getPic());
        if (resId != 0) {
            ivPic.setImageResource(resId);
        }

        ((TextView) findViewById(R.id.tv_detail_name)).setText(goods.getName());
        ((TextView) findViewById(R.id.tv_detail_desc)).setText(goods.getDesc());
        ((TextView) findViewById(R.id.tv_detail_sales)).setText("月售 " + goods.getSales());
        tvPrice = findViewById(R.id.tv_detail_price);

        TextView tvOriginal = findViewById(R.id.tv_detail_original);
        if (goods.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
            tvOriginal.setVisibility(View.VISIBLE);
            tvOriginal.setText("¥" + goods.getOriginalPrice().toPlainString());
            tvOriginal.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            tvOriginal.setVisibility(View.GONE);
        }
    }

    /** 初始化四组规格选项 */
    private void initSpecs() {
        LinearLayout llSizes = findViewById(R.id.ll_sizes);
        List<OptionBean> sizes = GoodsDataUtils.getSizes(this);
        for (int i = 0; i < sizes.size(); i++) {
            final OptionBean option = sizes.get(i);
            String text = option.getName()
                    + (option.getExtra().compareTo(BigDecimal.ZERO) > 0
                    ? " +¥" + option.getExtra().intValue() : "");
            TextView chip = createChip(llSizes, text);
            if (i == 0) {
                selectChip(chip, true);
                selectedSize = option;
            }
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 单选：先把同组全部取消，再选中自己
                    for (int j = 0; j < llSizes.getChildCount(); j++) {
                        selectChip((TextView) llSizes.getChildAt(j), false);
                    }
                    selectChip((TextView) v, true);
                    selectedSize = option;
                    updatePrice();
                }
            });
        }

        LinearLayout llTemps = findViewById(R.id.ll_temps);
        List<String> temps = GoodsDataUtils.getTemps(this);
        for (int i = 0; i < temps.size(); i++) {
            final String temp = temps.get(i);
            TextView chip = createChip(llTemps, temp);
            if (i == 0) {
                selectChip(chip, true);
                selectedTemp = temp;
            }
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int j = 0; j < llTemps.getChildCount(); j++) {
                        selectChip((TextView) llTemps.getChildAt(j), false);
                    }
                    selectChip((TextView) v, true);
                    selectedTemp = temp;
                }
            });
        }

        LinearLayout llSugars = findViewById(R.id.ll_sugars);
        List<String> sugars = GoodsDataUtils.getSugars(this);
        for (int i = 0; i < sugars.size(); i++) {
            final String sugar = sugars.get(i);
            TextView chip = createChip(llSugars, sugar);
            // 默认七分糖（下标 1），没有就默认第一个
            if (i == Math.min(1, sugars.size() - 1)) {
                selectChip(chip, true);
                selectedSugar = sugar;
            }
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int j = 0; j < llSugars.getChildCount(); j++) {
                        selectChip((TextView) llSugars.getChildAt(j), false);
                    }
                    selectChip((TextView) v, true);
                    selectedSugar = sugar;
                }
            });
        }

        LinearLayout llToppings = findViewById(R.id.ll_toppings);
        List<OptionBean> toppings = GoodsDataUtils.getToppings(this);
        for (int i = 0; i < toppings.size(); i++) {
            final OptionBean option = toppings.get(i);
            TextView chip = createChip(llToppings, option.getName()
                    + " +¥" + option.getExtra().intValue());
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 加料是多选：点一下切换
                    boolean selected = !v.isSelected();
                    selectChip((TextView) v, selected);
                    if (selected) {
                        selectedToppings.add(option);
                    } else {
                        selectedToppings.remove(option);
                    }
                    updatePrice();
                }
            });
        }
    }

    private void initBottomBar() {
        tvQuantity = findViewById(R.id.tv_qty_value);
        tvAddCart = findViewById(R.id.tv_add_cart);

        findViewById(R.id.tv_qty_minus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (quantity > 1) {
                    quantity--;
                    tvQuantity.setText(String.valueOf(quantity));
                    updatePrice();
                }
            }
        });
        findViewById(R.id.tv_qty_plus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (quantity < 20) {
                    quantity++;
                    tvQuantity.setText(String.valueOf(quantity));
                    updatePrice();
                }
            }
        });

        tvAddCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToCart(false);
            }
        });
        findViewById(R.id.tv_buy_now).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToCart(true);
            }
        });
    }

    /** 生成一个规格 chip 并加进容器 */
    private TextView createChip(LinearLayout parent, String text) {
        TextView chip = (TextView) LayoutInflater.from(this)
                .inflate(R.layout.item_chip, parent, false);
        chip.setText(text);
        parent.addView(chip);
        return chip;
    }

    /** 选中态：背景由 selector 切换，文字颜色要手动改 */
    private void selectChip(TextView chip, boolean selected) {
        chip.setSelected(selected);
        chip.setTextColor(getResources().getColor(
                selected ? R.color.white : R.color.text_black));
    }

    /** 当前单价（不含数量） */
    private BigDecimal unitPrice() {
        BigDecimal price = goods.getPrice();
        if (selectedSize != null) {
            price = price.add(selectedSize.getExtra());
        }
        for (OptionBean topping : selectedToppings) {
            price = price.add(topping.getExtra());
        }
        return price;
    }

    /** 每改一次规格/数量就刷新价格 */
    private void updatePrice() {
        BigDecimal unit = unitPrice();
        tvPrice.setText(PriceUtils.format(unit));
        tvAddCart.setText("加入购物车 · " + PriceUtils.format(unit.multiply(
                new BigDecimal(quantity))));
    }

    /** 规格文本：大杯/少冰/半糖/加珍珠、椰果 */
    private String buildSpecText() {
        StringBuilder sb = new StringBuilder();
        sb.append(selectedSize == null ? "中杯" : selectedSize.getName())
                .append("/").append(selectedTemp)
                .append("/").append(selectedSugar);
        if (!selectedToppings.isEmpty()) {
            sb.append("/加");
            for (int i = 0; i < selectedToppings.size(); i++) {
                if (i > 0) {
                    sb.append("、");
                }
                sb.append(selectedToppings.get(i).getName());
            }
        }
        return sb.toString();
    }

    private void addToCart(boolean buyNow) {
        CartManager.add(goods, buildSpecText(), unitPrice(), quantity);
        if (buyNow) {
            startActivity(new Intent(this, ConfirmOrderActivity.class));
            finish();
        } else {
            Toast.makeText(this, "已加入购物车", Toast.LENGTH_SHORT).show();
        }
    }
}
