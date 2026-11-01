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
import cn.itcast.order.bean.CouponBean;

/**
 * 优惠券列表适配器
 */
public class CouponAdapter extends BaseAdapter {

    private final Context context;
    private List<CouponBean> list = new ArrayList<>();

    public CouponAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<CouponBean> data) {
        list = data == null ? new ArrayList<CouponBean>() : data;
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
            convertView = LayoutInflater.from(context).inflate(R.layout.item_coupon, parent, false);
            holder = new ViewHolder();
            holder.tvAmount = convertView.findViewById(R.id.tv_coupon_amount);
            holder.tvThreshold = convertView.findViewById(R.id.tv_coupon_threshold);
            holder.tvTitle = convertView.findViewById(R.id.tv_coupon_title);
            holder.tvScope = convertView.findViewById(R.id.tv_coupon_scope);
            holder.tvState = convertView.findViewById(R.id.tv_coupon_state);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        CouponBean bean = list.get(position);
        holder.tvAmount.setText(bean.getAmountText());
        holder.tvThreshold.setText(bean.getThresholdText());
        holder.tvTitle.setText(bean.getTitle());
        holder.tvScope.setText("仅限本店使用 · " + bean.getScope());

        if (bean.isUsable()) {
            holder.tvState.setText("点击使用");
            holder.tvState.setTextColor(context.getResources().getColor(R.color.green));
            convertView.setAlpha(1f);
        } else {
            holder.tvState.setText(bean.getUnusableText());
            holder.tvState.setTextColor(context.getResources().getColor(R.color.text_light));
            convertView.setAlpha(0.5f);
        }
        return convertView;
    }

    static class ViewHolder {
        TextView tvAmount;
        TextView tvThreshold;
        TextView tvTitle;
        TextView tvScope;
        TextView tvState;
    }
}
