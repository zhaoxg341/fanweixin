package cn.itcast.weixin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.weixin.R;
import cn.itcast.weixin.bean.ChatBean;
import cn.itcast.weixin.db.AvatarDao;
import cn.itcast.weixin.db.RemarkDao;
import cn.itcast.weixin.utils.AvatarStore;
import cn.itcast.weixin.view.CircleImageView;

/**
 * 微信 Tab 会话列表适配器（ViewHolder 模式复用控件）
 */
public class ChatAdapter extends BaseAdapter {

    private final Context context;
    private List<ChatBean> chatList = new ArrayList<>();

    public ChatAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<ChatBean> chatList) {
        this.chatList = chatList == null ? new ArrayList<ChatBean>() : chatList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return chatList.size();
    }

    @Override
    public Object getItem(int position) {
        return chatList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
            holder = new ViewHolder();
            holder.ivAvatar = convertView.findViewById(R.id.iv_avatar);
            holder.tvUnread = convertView.findViewById(R.id.tv_unread);
            holder.tvName = convertView.findViewById(R.id.tv_name);
            holder.tvMsg = convertView.findViewById(R.id.tv_msg);
            holder.tvTime = convertView.findViewById(R.id.tv_time);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ChatBean chat = chatList.get(position);
        // 备注优先：给联系人加过备注后，会话列表里的名字也会跟着改
        holder.tvName.setText(RemarkDao.displayName(chat.getName()));
        holder.tvMsg.setText(chat.getLastMsg());
        holder.tvTime.setText(chat.getTime());

        // 头像也走统一出口：内置头像和相册照片都能显示
        String avatar = AvatarDao.displayAvatar(chat.getName(), chat.getAvatar());
        AvatarStore.apply(holder.ivAvatar, avatar, R.drawable.avatar_default);

        if (chat.getUnread() > 0) {
            holder.tvUnread.setVisibility(View.VISIBLE);
            holder.tvUnread.setText(chat.getUnread() > 99 ? "99+" : String.valueOf(chat.getUnread()));
        } else {
            holder.tvUnread.setVisibility(View.GONE);
        }
        return convertView;
    }

    static class ViewHolder {
        CircleImageView ivAvatar;
        TextView tvUnread;
        TextView tvName;
        TextView tvMsg;
        TextView tvTime;
    }
}
