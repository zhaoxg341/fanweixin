package cn.itcast.milktea.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.milktea.R;
import cn.itcast.milktea.bean.GoodsBean;
import cn.itcast.milktea.utils.CartManager;
import cn.itcast.milktea.utils.GoodsDataUtils;

/**
 * 商品列表适配器（首页和搜索页共用）。
 *
 * 数字加减不在这里存状态，数量永远从 CartManager 查，
 * 这样从详情页返回后调用 notifyDataSetChanged() 就能拿到最新数量。
 */
public class GoodsAdapter extends BaseAdapter {

    public interface OnGoodsActionListener {
        /** 点击商品，进详情页选规格 */
        void onGoodsClick(GoodsBean goods);

        /** 购物车变了，让页面刷新底部结算栏 */
        void onCartChanged();
    }

    private final Context context;
    private final OnGoodsActionListener listener;
    private List<GoodsBean> data = new ArrayList<>();

    public GoodsAdapter(Context context, OnGoodsActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<GoodsBean> data) {
        this.data = data == null ? new ArrayList<GoodsBean>() : data;
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
            convertView = LayoutInflater.from(context).inflate(R.layout.item_goods, parent, false);
            holder = new ViewHolder();
            holder.ivPic = convertView.findViewById(R.id.iv_goods_pic);
            holder.tvName = convertView.findViewById(R.id.tv_goods_name);
            holder.tvDesc = convertView.findViewById(R.id.tv_goods_desc);
            holder.tvBadge = convertView.findViewById(R.id.tv_goods_badge);
            holder.tvSales = convertView.findViewById(R.id.tv_goods_sales);
            holder.tvPrice = convertView.findViewById(R.id.tv_goods_price);
            holder.tvOriginal = convertView.findViewById(R.id.tv_original_price);
            holder.tvReduce = convertView.findViewById(R.id.tv_reduce);
            holder.tvCount = convertView.findViewById(R.id.tv_goods_count);
            holder.tvAdd = convertView.findViewById(R.id.tv_add);
            // 商品图圆角：给 ImageView 一个圆角背景，再让内容裁剪到背景轮廓
            holder.ivPic.setBackgroundResource(R.drawable.bg_card_white);
            holder.ivPic.setClipToOutline(true);
            // 划线价
            holder.tvOriginal.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final GoodsBean goods = data.get(position);
        holder.tvName.setText(goods.getName());
        holder.tvDesc.setText(goods.getDesc());
        holder.tvPrice.setText(goods.getPriceString());

        int picRes = GoodsDataUtils.getDrawableId(context, goods.getPic());
        if (picRes != 0) {
            holder.ivPic.setImageResource(picRes);
        }

        if (goods.hasBadge()) {
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText(goods.getBadge());
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }
        holder.tvSales.setText("月售" + goods.getSales());

        if (goods.getOriginalPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
            holder.tvOriginal.setVisibility(View.VISIBLE);
            holder.tvOriginal.setText("¥" + goods.getOriginalPrice().toPlainString());
        } else {
            holder.tvOriginal.setVisibility(View.GONE);
        }

        int count = CartManager.getCountByGoods(goods.getGoodsId());
        if (count > 0) {
            holder.tvCount.setVisibility(View.VISIBLE);
            holder.tvReduce.setVisibility(View.VISIBLE);
            holder.tvCount.setText(String.valueOf(count));
        } else {
            holder.tvCount.setVisibility(View.GONE);
            holder.tvReduce.setVisibility(View.GONE);
        }

        holder.tvAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CartManager.addDefault(goods);
                notifyDataSetChanged();
                listener.onCartChanged();
            }
        });

        holder.tvReduce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CartManager.decreaseByGoods(goods.getGoodsId());
                notifyDataSetChanged();
                listener.onCartChanged();
            }
        });

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onGoodsClick(goods);
            }
        });
        return convertView;
    }

    static class ViewHolder {
        ImageView ivPic;
        TextView tvName;
        TextView tvDesc;
        TextView tvBadge;
        TextView tvSales;
        TextView tvPrice;
        TextView tvOriginal;
        TextView tvReduce;
        TextView tvCount;
        TextView tvAdd;
    }
}
