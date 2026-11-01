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
import cn.itcast.order.bean.AddressBean;

/**
 * 收货地址列表适配器
 */
public class AddressAdapter extends BaseAdapter {

    public interface OnEditListener {
        void onEdit(AddressBean bean);
    }

    private final Context context;
    private final OnEditListener listener;
    private List<AddressBean> list = new ArrayList<>();

    public AddressAdapter(Context context, OnEditListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<AddressBean> data) {
        list = data == null ? new ArrayList<AddressBean>() : data;
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
            convertView = LayoutInflater.from(context).inflate(R.layout.item_address, parent, false);
            holder = new ViewHolder();
            holder.tvName = convertView.findViewById(R.id.tv_address_name);
            holder.tvPhone = convertView.findViewById(R.id.tv_address_phone);
            holder.tvDetail = convertView.findViewById(R.id.tv_address_detail);
            holder.tvDefault = convertView.findViewById(R.id.tv_address_default);
            holder.tvAction = convertView.findViewById(R.id.tv_address_action);
            holder.tvEdit = convertView.findViewById(R.id.tv_address_edit);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final AddressBean bean = list.get(position);
        holder.tvName.setText(bean.getName());
        holder.tvPhone.setText(bean.getPhone());
        holder.tvDetail.setText(bean.getDetail());
        holder.tvDefault.setVisibility(bean.isDefault() ? View.VISIBLE : View.GONE);
        holder.tvEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onEdit(bean);
                }
            }
        });
        return convertView;
    }

    static class ViewHolder {
        TextView tvName;
        TextView tvPhone;
        TextView tvDetail;
        TextView tvDefault;
        TextView tvAction;
        TextView tvEdit;
    }
}
