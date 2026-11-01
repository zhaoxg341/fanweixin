package cn.itcast.order.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.itcast.order.R;
import cn.itcast.order.bean.OrderBean;
import cn.itcast.order.utils.ShopDataUtils;

/**
 * 订单中心列表适配器
 */
public class OrderCenterAdapter extends BaseAdapter {

    public interface OnOrderActionListener {
        void onCancel(OrderBean order);

        void onPay(OrderBean order);

        void onRebuy(OrderBean order);

        void onDetail(OrderBean order);
    }

    private final Context context;
    private final OnOrderActionListener listener;
    private List<OrderBean> list = new ArrayList<>();

    public OrderCenterAdapter(Context context, OnOrderActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<OrderBean> data) {
        list = data == null ? new ArrayList<OrderBean>() : data;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_order_center, parent, false);
            holder = new ViewHolder();
            holder.ivShop = convertView.findViewById(R.id.iv_order_shop);
            holder.tvShopName = convertView.findViewById(R.id.tv_order_shop_name);
            holder.tvStatus = convertView.findViewById(R.id.tv_order_status);
            holder.tvSummary = convertView.findViewById(R.id.tv_order_summary);
            holder.tvTotal = convertView.findViewById(R.id.tv_order_total);
            holder.tvTime = convertView.findViewById(R.id.tv_order_time);
            holder.tvAction1 = convertView.findViewById(R.id.tv_action_1);
            holder.tvAction2 = convertView.findViewById(R.id.tv_action_2);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final OrderBean order = list.get(position);
        holder.tvShopName.setText(order.getShopName());
        holder.tvSummary.setText(order.getSummary());
        holder.tvTotal.setText(order.getTotalText());
        holder.tvTime.setText(new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
                .format(new Date(order.getCreateTime())));

        holder.tvStatus.setText(order.getStatusText());
        holder.tvStatus.setTextColor(context.getResources().getColor(statusColor(order.getStatus())));

        int resId = ShopDataUtils.getDrawableId(context, order.getShopBanner());
        if (resId != 0) {
            holder.ivShop.setImageResource(resId);
        }

        // 待支付：取消 + 去支付
        if (order.getStatus() == OrderBean.STATUS_UNPAID) {
            holder.tvAction1.setVisibility(View.VISIBLE);
            holder.tvAction1.setText("取消订单");
            holder.tvAction1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onCancel(order);
                    }
                }
            });
            holder.tvAction2.setVisibility(View.VISIBLE);
            holder.tvAction2.setText("去支付");
            holder.tvAction2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onPay(order);
                    }
                }
            });
        } else if (order.getStatus() == OrderBean.STATUS_DELIVERING) {
            holder.tvAction1.setVisibility(View.VISIBLE);
            holder.tvAction1.setText("取消订单");
            holder.tvAction1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onCancel(order);
                    }
                }
            });
            holder.tvAction2.setVisibility(View.VISIBLE);
            holder.tvAction2.setText("查看详情");
            holder.tvAction2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onDetail(order);
                    }
                }
            });
        } else {
            holder.tvAction1.setVisibility(View.GONE);
            holder.tvAction2.setVisibility(View.VISIBLE);
            holder.tvAction2.setText("再来一单");
            holder.tvAction2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onRebuy(order);
                    }
                }
            });
        }
        return convertView;
    }

    private int statusColor(int status) {
        switch (status) {
            case OrderBean.STATUS_UNPAID:
                return R.color.orange;
            case OrderBean.STATUS_DELIVERING:
                return R.color.blue;
            default:
                return R.color.text_gray;
        }
    }

    static class ViewHolder {
        ImageView ivShop;
        TextView tvShopName;
        TextView tvStatus;
        TextView tvSummary;
        TextView tvTotal;
        TextView tvTime;
        TextView tvAction1;
        TextView tvAction2;
    }
}
