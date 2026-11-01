package cn.itcast.weixin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import cn.itcast.weixin.R;
import cn.itcast.weixin.db.AvatarDao;
import cn.itcast.weixin.utils.WeixinDataUtils;
import cn.itcast.weixin.view.CircleImageView;

/**
 * 头像选择器的网格适配器。
 * 数据源固定是 AvatarDao.POOL（16 张候选头像），当前选中的那张外面套一圈绿环。
 */
public class AvatarPickerAdapter extends BaseAdapter {

    private final Context context;
    private String selected;

    public AvatarPickerAdapter(Context context, String selected) {
        this.context = context;
        this.selected = AvatarDao.normalize(selected);
    }

    /** 选中项变了，重画一遍让绿环跟着移动 */
    public void setSelected(String selected) {
        this.selected = AvatarDao.normalize(selected);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return AvatarDao.POOL.length;
    }

    @Override
    public Object getItem(int position) {
        return AvatarDao.POOL[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_avatar_choice, parent, false);
            holder = new ViewHolder();
            holder.ivChoice = convertView.findViewById(R.id.iv_choice);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String name = AvatarDao.POOL[position];
        int resId = WeixinDataUtils.getDrawableId(context, name);
        if (resId != 0) {
            holder.ivChoice.setImageResource(resId);
        }

        // 选中的那张加绿环；没选中就清掉背景
        boolean isSelected = name.equals(selected);
        convertView.setBackgroundResource(isSelected ? R.drawable.bg_avatar_selected : 0);
        return convertView;
    }

    static class ViewHolder {
        CircleImageView ivChoice;
    }
}
