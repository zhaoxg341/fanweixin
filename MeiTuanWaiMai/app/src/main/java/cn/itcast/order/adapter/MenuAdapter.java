package cn.itcast.order.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.order.R;
import cn.itcast.order.bean.FoodBean;
import cn.itcast.order.utils.CartManager;
import cn.itcast.order.utils.ShopDataUtils;

/**
 * 菜单列表的数据适配器（对应教材 12.5.5 编写菜单列表的数据适配器）
 *
 * 每个条目显示：菜品图片、名称、人气标签、月售量、价格，以及“+ / −”数量控件。
 * 数量的唯一数据来源是 CartManager，适配器本身不保存状态，
 * 这样从菜品详情页返回后调用 notifyDataSetChanged() 就能拿到最新数量。
 */
public class MenuAdapter extends BaseAdapter {

    /** 条目点击（进入菜品详情）与购物车变化的回调 */
    public interface OnMenuListener {
        void onFoodClick(FoodBean food);

        void onCartChanged();
    }

    private final Context context;
    private final OnMenuListener listener;
    private List<FoodBean> foodList = new ArrayList<>();

    public MenuAdapter(Context context, OnMenuListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<FoodBean> foodList) {
        this.foodList = foodList == null ? new ArrayList<FoodBean>() : foodList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return foodList.size();
    }

    @Override
    public Object getItem(int position) {
        return foodList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.menu_item, parent, false);
            holder = new ViewHolder();
            holder.ivFoodPic = convertView.findViewById(R.id.iv_food_pic);
            holder.tvFoodName = convertView.findViewById(R.id.tv_food_name);
            holder.tvPopularity = convertView.findViewById(R.id.tv_popularity);
            holder.tvFoodSale = convertView.findViewById(R.id.tv_food_sale);
            holder.tvFoodPrice = convertView.findViewById(R.id.tv_food_price);
            holder.tvReduce = convertView.findViewById(R.id.tv_reduce);
            holder.tvFoodCount = convertView.findViewById(R.id.tv_food_count);
            holder.tvAdd = convertView.findViewById(R.id.tv_add);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final FoodBean food = foodList.get(position);
        holder.tvFoodName.setText(food.getFoodName());
        holder.tvPopularity.setText(food.getPopularity());
        holder.tvFoodSale.setText(String.format("月售%d", food.getSaleNum()));
        holder.tvFoodPrice.setText(food.getPriceString());

        int resId = ShopDataUtils.getDrawableId(context, food.getFoodPic());
        if (resId != 0) {
            holder.ivFoodPic.setImageResource(resId);
        }

        // 显示购物车中的数量：为 0 时隐藏“−”和数字
        int count = CartManager.getCount(food.getFoodId());
        if (count > 0) {
            holder.tvFoodCount.setVisibility(View.VISIBLE);
            holder.tvReduce.setVisibility(View.VISIBLE);
            holder.tvFoodCount.setText(String.valueOf(count));
        } else {
            holder.tvFoodCount.setVisibility(View.GONE);
            holder.tvReduce.setVisibility(View.GONE);
        }

        // 点击“+”加入购物车
        holder.tvAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CartManager.add(food);
                notifyDataSetChanged();
                listener.onCartChanged();
            }
        });

        // 点击“−”减少数量
        holder.tvReduce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CartManager.reduce(food.getFoodId());
                notifyDataSetChanged();
                listener.onCartChanged();
            }
        });

        // 点击条目其它区域进入菜品详情
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onFoodClick(food);
            }
        });
        return convertView;
    }

    static class ViewHolder {
        ImageView ivFoodPic;
        TextView tvFoodName;
        TextView tvPopularity;
        TextView tvFoodSale;
        TextView tvFoodPrice;
        TextView tvReduce;
        TextView tvFoodCount;
        TextView tvAdd;
    }
}
