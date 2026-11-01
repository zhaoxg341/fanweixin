package cn.itcast.weixin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.itcast.weixin.R;
import cn.itcast.weixin.bean.ContactBean;
import cn.itcast.weixin.db.AvatarDao;
import cn.itcast.weixin.utils.AvatarStore;
import cn.itcast.weixin.view.CircleImageView;

/**
 * 通讯录适配器。三种行样式：
 * 0 功能入口（新的朋友 / 群聊 / 标签 / 公众号）
 * 1 字母分组标题
 * 2 联系人
 */
public class ContactAdapter extends BaseAdapter {

    public static class Entry {
        public final String title;
        public final int iconRes;

        public Entry(String title, int iconRes) {
            this.title = title;
            this.iconRes = iconRes;
        }
    }

    private static final int TYPE_ENTRY = 0;
    private static final int TYPE_HEADER = 1;
    private static final int TYPE_CONTACT = 2;

    private final Context context;
    private final List<Entry> entries;
    private final List<Object> rows = new ArrayList<>();
    private final Map<String, Integer> letterPositions = new HashMap<>();

    public ContactAdapter(Context context, List<Entry> entries) {
        this.context = context;
        this.entries = entries;
        buildRows(new ArrayList<ContactBean>());
    }

    /** 设置联系人数据，内部自动按字母分组 */
    public void setContacts(List<ContactBean> contacts) {
        buildRows(contacts);
        notifyDataSetChanged();
    }

    private void buildRows(List<ContactBean> contacts) {
        rows.clear();
        letterPositions.clear();
        rows.addAll(entries);
        if (contacts == null) {
            return;
        }
        List<ContactBean> sorted = new ArrayList<>(contacts);
        Collections.sort(sorted, new Comparator<ContactBean>() {
            @Override
            public int compare(ContactBean o1, ContactBean o2) {
                int c = o1.getLetter().compareTo(o2.getLetter());
                return c != 0 ? c : o1.getName().compareTo(o2.getName());
            }
        });
        String lastLetter = null;
        for (ContactBean c : sorted) {
            String letter = c.getLetter();
            if (!letter.equals(lastLetter)) {
                letterPositions.put(letter, rows.size());
                rows.add(letter);
                lastLetter = letter;
            }
            rows.add(c);
        }
    }

    /** 字母索引栏跳转用：拿到某个字母分组所在的行号 */
    public int getPositionForLetter(String letter) {
        Integer p = letterPositions.get(letter);
        return p == null ? -1 : p;
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
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        Object o = rows.get(position);
        if (o instanceof Entry) {
            return TYPE_ENTRY;
        } else if (o instanceof String) {
            return TYPE_HEADER;
        }
        return TYPE_CONTACT;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int type = getItemViewType(position);
        if (type == TYPE_HEADER) {
            TextView tv = (TextView) convertView;
            if (tv == null) {
                tv = (TextView) LayoutInflater.from(context).inflate(R.layout.item_contact_header, parent, false);
            }
            tv.setText((String) rows.get(position));
            return tv;
        }
        if (type == TYPE_ENTRY) {
            EntryHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_contact_entry, parent, false);
                holder = new EntryHolder();
                holder.ivIcon = convertView.findViewById(R.id.iv_icon);
                holder.tvName = convertView.findViewById(R.id.tv_name);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }
            Entry entry = (Entry) rows.get(position);
            holder.ivIcon.setImageResource(entry.iconRes);
            holder.tvName.setText(entry.title);
            return convertView;
        }

        ContactHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false);
            holder = new ContactHolder();
            holder.ivAvatar = convertView.findViewById(R.id.iv_avatar);
            holder.tvName = convertView.findViewById(R.id.tv_name);
            convertView.setTag(holder);
        } else {
            holder = (ContactHolder) convertView.getTag();
        }
        ContactBean contact = (ContactBean) rows.get(position);
        // 有备注就显示备注
        holder.tvName.setText(contact.getDisplayName());
        // 头像走统一出口：内置头像和相册照片都能显示
        String avatar = AvatarDao.displayAvatar(contact.getName(), contact.getAvatar());
        AvatarStore.apply(holder.ivAvatar, avatar, R.drawable.avatar_default);
        return convertView;
    }

    static class EntryHolder {
        ImageView ivIcon;
        TextView tvName;
    }

    static class ContactHolder {
        CircleImageView ivAvatar;
        TextView tvName;
    }
}
