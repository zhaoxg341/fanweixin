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
import cn.itcast.order.utils.ShopDataUtils;

/**
 * 订单列表的数据适配器（对应教材 12.7.4 编写订单列表的数据适配器）
 * 订单确认页展示购物车中已选商品：图片、名称、单价、数量、小计
 */
public class OrderAdapter extends BaseAdapter {

    private final Context context;
    private List<FoodBean> orderList = new ArrayList<>();

    public OrderAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<FoodBean> orderList) {
        this.orderList = orderList == null ? new ArrayList<FoodBean>() : orderList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return orderList.size();
    }

    @Override
    public Object getItem(int position) {
        return orderList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.order_item, parent, false);
            holder = new ViewHolder();
            holder.ivFoodPic = convertView.findViewById(R.id.iv_order_food_pic);
            holder.tvFoodName = convertView.findViewById(R.id.tv_order_food_name);
            holder.tvFoodPrice = convertView.findViewById(R.id.tv_order_food_price);
            holder.tvCount = convertView.findViewById(R.id.tv_order_food_count);
            holder.tvSubtotal = convertView.findViewById(R.id.tv_order_food_subtotal);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        FoodBean food = orderList.get(position);
        holder.tvFoodName.setText(food.getFoodName());
        holder.tvFoodPrice.setText(food.getPriceString());
        holder.tvCount.setText(String.format("x%d", food.getCount()));
        holder.tvSubtotal.setText("¥" + food.getSubtotal().toPlainString());

        int resId = ShopDataUtils.getDrawableId(context, food.getFoodPic());
        if (resId != 0) {
            holder.ivFoodPic.setImageResource(resId);
        }
        return convertView;
    }

    static class ViewHolder {
        ImageView ivFoodPic;
        TextView tvFoodName;
        TextView tvFoodPrice;
        TextView tvCount;
        TextView tvSubtotal;
    }
}
