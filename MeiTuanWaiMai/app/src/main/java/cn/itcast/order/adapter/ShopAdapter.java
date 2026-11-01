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
import cn.itcast.order.bean.ShopBean;
import cn.itcast.order.utils.ShopDataUtils;

/**
 * 店铺列表的数据适配器（对应教材 12.4.7）
 * 使用 ViewHolder 模式缓存控件，避免每次 getView() 都调用 findViewById
 */
public class ShopAdapter extends BaseAdapter {

    private final Context context;
    private List<ShopBean> shopList = new ArrayList<>();

    public ShopAdapter(Context context) {
        this.context = context;
    }

    /** 设置店铺数据 */
    public void setData(List<ShopBean> shopList) {
        this.shopList = shopList == null ? new ArrayList<ShopBean>() : shopList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return shopList.size();
    }

    @Override
    public Object getItem(int position) {
        return shopList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.shop_item, parent, false);
            holder = new ViewHolder();
            holder.ivShopPic = convertView.findViewById(R.id.iv_shop_pic);
            holder.tvShopName = convertView.findViewById(R.id.tv_shop_name);
            holder.tvScore = convertView.findViewById(R.id.tv_score);
            holder.tvSaleNum = convertView.findViewById(R.id.tv_sale_num);
            holder.tvTime = convertView.findViewById(R.id.tv_time);
            holder.tvOffer = convertView.findViewById(R.id.tv_offer);
            holder.tvFeature = convertView.findViewById(R.id.tv_feature);
            holder.tvDistribution = convertView.findViewById(R.id.tv_distribution);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ShopBean shop = shopList.get(position);
        holder.tvShopName.setText(shop.getShopName());
        holder.tvScore.setText(String.format("★ %.1f", shop.getScore()));
        holder.tvSaleNum.setText(String.format("月售%d", shop.getSaleNum()));
        holder.tvTime.setText(shop.getTime());
        holder.tvOffer.setText(shop.getOfferText());
        holder.tvFeature.setText(shop.getFeature());
        holder.tvDistribution.setText(shop.getDistributionText());

        int resId = ShopDataUtils.getDrawableId(context, shop.getBanner());
        if (resId != 0) {
            holder.ivShopPic.setImageResource(resId);
        }
        return convertView;
    }

    static class ViewHolder {
        ImageView ivShopPic;
        TextView tvShopName;
        TextView tvScore;
        TextView tvSaleNum;
        TextView tvTime;
        TextView tvOffer;
        TextView tvFeature;
        TextView tvDistribution;
    }
}
