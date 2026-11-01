package cn.itcast.milktea.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.milktea.R;
import cn.itcast.milktea.bean.OrderBean;
import cn.itcast.milktea.bean.OrderItemBean;
import cn.itcast.milktea.utils.PriceUtils;

/**
 * 订单列表适配器。
 *
 * 每张卡片显示：订单号、状态、商品摘要（最多 3 行）、时间、件数、实付金额，
 * 底部两个按钮的文案随状态变化：
 *   待支付 → [取消订单] [去支付]
 *   制作中 → [查看详情] [确认取餐]
 *   已完成 → [查看详情] [再来一单]
 *   已取消 → [查看详情] [再来一单]
 */
public class OrderAdapter extends BaseAdapter {

    public interface OnOrderActionListener {
        /** 点卡片：进详情 */
        void onOrderClick(OrderBean order);

        /** 左边按钮 */
        void onLeftAction(OrderBean order);

        /** 右边按钮 */
        void onRightAction(OrderBean order);
    }

    private final Context context;
    private final OnOrderActionListener listener;
    private List<OrderBean> data = new ArrayList<>();

    public OrderAdapter(Context context, OnOrderActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<OrderBean> data) {
        this.data = data == null ? new ArrayList<OrderBean>() : data;
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
            convertView = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
            holder = new ViewHolder();
            holder.tvNo = convertView.findViewById(R.id.tv_order_no);
            holder.tvStatus = convertView.findViewById(R.id.tv_order_status);
            holder.tvGoods = convertView.findViewById(R.id.tv_order_goods);
            holder.tvTime = convertView.findViewById(R.id.tv_order_time);
            holder.tvCount = convertView.findViewById(R.id.tv_order_count);
            holder.tvTotal = convertView.findViewById(R.id.tv_order_total);
            holder.tvLeft = convertView.findViewById(R.id.tv_order_left);
            holder.tvRight = convertView.findViewById(R.id.tv_order_right);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final OrderBean order = data.get(position);
        holder.tvNo.setText("订单号 " + order.getOrderNo());
        holder.tvStatus.setText(order.getStatusText());
        holder.tvTime.setText(PriceUtils.formatTime(order.getCreatedAt()));
        holder.tvCount.setText(String.valueOf(order.getItemCount()));
        holder.tvTotal.setText(PriceUtils.format(order.getTotal()));

        // 状态颜色：待支付红色提醒，制作中主色，已完成灰色
        int color;
        switch (order.getStatus()) {
            case OrderBean.STATUS_UNPAID:
                color = context.getResources().getColor(R.color.badge_red);
                break;
            case OrderBean.STATUS_MAKING:
                color = context.getResources().getColor(R.color.primary);
                break;
            case OrderBean.STATUS_DONE:
                color = context.getResources().getColor(R.color.tag_green);
                break;
            default:
                color = Color.GRAY;
                break;
        }
        holder.tvStatus.setTextColor(color);

        // 商品摘要：名称 x数量，用顿号连起来
        StringBuilder sb = new StringBuilder();
        for (OrderItemBean item : order.getItems()) {
            if (sb.length() > 0) {
                sb.append("、");
            }
            sb.append(item.getName()).append(" x").append(item.getCount());
        }
        holder.tvGoods.setText(sb.toString());

        if (order.getStatus() == OrderBean.STATUS_UNPAID) {
            holder.tvLeft.setText("取消订单");
            holder.tvRight.setText("去支付");
        } else if (order.getStatus() == OrderBean.STATUS_MAKING) {
            holder.tvLeft.setText(R.string.view_order);
            holder.tvRight.setText(R.string.confirm_pickup);
        } else {
            holder.tvLeft.setText(R.string.view_order);
            holder.tvRight.setText(R.string.order_again);
        }

        holder.tvLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onLeftAction(order);
            }
        });
        holder.tvRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onRightAction(order);
            }
        });
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onOrderClick(order);
            }
        });
        return convertView;
    }

    static class ViewHolder {
        TextView tvNo;
        TextView tvStatus;
        TextView tvGoods;
        TextView tvTime;
        TextView tvCount;
        TextView tvTotal;
        TextView tvLeft;
        TextView tvRight;
    }
}
