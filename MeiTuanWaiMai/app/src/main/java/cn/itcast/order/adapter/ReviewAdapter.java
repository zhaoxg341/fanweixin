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
import cn.itcast.order.bean.ReviewBean;

/**
 * 评价列表适配器
 */
public class ReviewAdapter extends BaseAdapter {

    private final Context context;
    private List<ReviewBean> list = new ArrayList<>();

    public ReviewAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<ReviewBean> data) {
        list = data == null ? new ArrayList<ReviewBean>() : data;
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
            convertView = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
            holder = new ViewHolder();
            holder.tvAvatar = convertView.findViewById(R.id.tv_avatar);
            holder.tvUserName = convertView.findViewById(R.id.tv_user_name);
            holder.tvScore = convertView.findViewById(R.id.tv_review_score);
            holder.tvTime = convertView.findViewById(R.id.tv_review_time);
            holder.tvContent = convertView.findViewById(R.id.tv_review_content);
            holder.tvTag = convertView.findViewById(R.id.tv_review_tag);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ReviewBean bean = list.get(position);
        holder.tvAvatar.setText(bean.getAvatarText());
        holder.tvUserName.setText(bean.getUserName());
        holder.tvScore.setText(stars(bean.getScore()));
        holder.tvTime.setText(bean.getTime());
        holder.tvContent.setText(bean.getContent());
        holder.tvTag.setText(bean.getTag());
        return convertView;
    }

    /** 4.5 分显示 4 颗实心星 + 1 颗空心星 */
    private String stars(float score) {
        int full = (int) score;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(i < full ? "★" : "☆");
        }
        return sb.toString();
    }

    static class ViewHolder {
        TextView tvAvatar;
        TextView tvUserName;
        TextView tvScore;
        TextView tvTime;
        TextView tvContent;
        TextView tvTag;
    }
}
