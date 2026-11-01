package cn.itcast.milktea.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.milktea.R;
import cn.itcast.milktea.bean.CartItem;
import cn.itcast.milktea.utils.CartManager;
import cn.itcast.milktea.utils.GoodsDataUtils;
import cn.itcast.milktea.utils.PriceUtils;

/**
 * 购物车列表适配器：每行一个"商品 + 规格"，加减数量直接改 CartManager。
 */
public class CartAdapter extends BaseAdapter {

    public interface OnCartChangedListener {
        void onCartChanged();
    }

    private final Context context;
    private final OnCartChangedListener listener;
    private List<CartItem> data = new ArrayList<>();

    public CartAdapter(Context context, OnCartChangedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<CartItem> data) {
        this.data = data == null ? new ArrayList<CartItem>() : data;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
            holder = new ViewHolder();
            holder.ivPic = convertView.findViewById(R.id.iv_cart_pic);
            holder.tvName = convertView.findViewById(R.id.tv_cart_name);
            holder.tvSpec = convertView.findViewById(R.id.tv_cart_spec);
            holder.tvPrice = convertView.findViewById(R.id.tv_cart_price);
            holder.tvMinus = convertView.findViewById(R.id.tv_cart_minus);
            holder.tvCount = convertView.findViewById(R.id.tv_cart_count);
            holder.tvPlus = convertView.findViewById(R.id.tv_cart_plus);
            holder.ivPic.setBackgroundResource(R.drawable.bg_card_white);
            holder.ivPic.setClipToOutline(true);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final CartItem item = data.get(position);
        holder.tvName.setText(item.getName());
        holder.tvSpec.setText(item.getSpecText());
        holder.tvPrice.setText(PriceUtils.format(item.getUnitPrice()));
        holder.tvCount.setText(String.valueOf(item.getCount()));

        int resId = GoodsDataUtils.getDrawableId(context, item.getPic());
        if (resId != 0) {
            holder.ivPic.setImageResource(resId);
        }

        holder.tvMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CartManager.decrease(item.getKey());
                setData(CartManager.getCartList());
                listener.onCartChanged();
            }
        });

        holder.tvPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CartManager.increase(item.getKey());
                setData(CartManager.getCartList());
                listener.onCartChanged();
            }
        });
        return convertView;
    }

    static class ViewHolder {
        ImageView ivPic;
        TextView tvName;
        TextView tvSpec;
        TextView tvPrice;
        TextView tvMinus;
        TextView tvCount;
        TextView tvPlus;
    }
}
