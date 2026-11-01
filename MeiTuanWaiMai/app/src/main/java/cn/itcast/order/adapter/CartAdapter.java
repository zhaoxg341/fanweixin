package cn.itcast.order.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.order.R;
import cn.itcast.order.bean.FoodBean;
import cn.itcast.order.utils.CartManager;

/**
 * 购物车列表的数据适配器（对应教材 12.5.6 编写购物车列表的数据适配器）
 * 用于店铺详情页弹出的购物车窗口
 */
public class CartAdapter extends BaseAdapter {

    public interface OnCartChangedListener {
        void onCartChanged();
    }

    private final Context context;
    private final OnCartChangedListener listener;
    private List<FoodBean> cartList = new ArrayList<>();

    public CartAdapter(Context context, OnCartChangedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<FoodBean> cartList) {
        this.cartList = cartList == null ? new ArrayList<FoodBean>() : cartList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return cartList.size();
    }

    @Override
    public Object getItem(int position) {
        return cartList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.cart_item, parent, false);
            holder = new ViewHolder();
            holder.tvFoodName = convertView.findViewById(R.id.tv_cart_food_name);
            holder.tvFoodPrice = convertView.findViewById(R.id.tv_cart_food_price);
            holder.tvReduce = convertView.findViewById(R.id.tv_cart_reduce);
            holder.tvCount = convertView.findViewById(R.id.tv_cart_count);
            holder.tvAdd = convertView.findViewById(R.id.tv_cart_add);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final FoodBean food = cartList.get(position);
        holder.tvFoodName.setText(food.getFoodName());
        holder.tvFoodPrice.setText(food.getPriceString());
        holder.tvCount.setText(String.valueOf(food.getCount()));

        holder.tvAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CartManager.add(food);
                listener.onCartChanged();
            }
        });

        holder.tvReduce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CartManager.reduce(food.getFoodId());
                listener.onCartChanged();
            }
        });
        return convertView;
    }

    static class ViewHolder {
        TextView tvFoodName;
        TextView tvFoodPrice;
        TextView tvReduce;
        TextView tvCount;
        TextView tvAdd;
    }
}
