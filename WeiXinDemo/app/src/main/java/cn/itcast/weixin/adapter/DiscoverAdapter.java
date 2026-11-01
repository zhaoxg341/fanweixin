package cn.itcast.weixin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.weixin.R;

/**
 * 发现页适配器。两种行样式：
 * 0 功能条目（彩色图标 + 名称 + 箭头）
 * 1 分组之间的灰色间隔
 */
public class DiscoverAdapter extends BaseAdapter {

    public static class Item {
        public final String title;
        public final int iconRes;

        public Item(String title, int iconRes) {
            this.title = title;
            this.iconRes = iconRes;
        }
    }

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_GAP = 1;
    /** 分组间隔的占位对象 */
    private static final Object GAP = new Object();

    private final Context context;
    private final List<Object> rows = new ArrayList<>();

    public DiscoverAdapter(Context context) {
        this.context = context;
    }

    /** 按分组设置数据，组与组之间自动插入灰色间隔 */
    public void setGroups(List<List<Item>> groups) {
        rows.clear();
        for (int i = 0; i < groups.size(); i++) {
            if (i > 0) {
                rows.add(GAP);
            }
            rows.addAll(groups.get(i));
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public Object getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position) == GAP ? TYPE_GAP : TYPE_ITEM;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == TYPE_GAP) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_gap, parent, false);
            }
            return convertView;
        }

        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_discover, parent, false);
            holder = new ViewHolder();
            holder.ivIcon = convertView.findViewById(R.id.iv_icon);
            holder.tvName = convertView.findViewById(R.id.tv_name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Item item = (Item) rows.get(position);
        holder.ivIcon.setImageResource(item.iconRes);
        holder.tvName.setText(item.title);
        return convertView;
    }

    static class ViewHolder {
        ImageView ivIcon;
        TextView tvName;
    }
}
