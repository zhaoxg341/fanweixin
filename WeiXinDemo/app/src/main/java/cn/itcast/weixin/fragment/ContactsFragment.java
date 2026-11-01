package cn.itcast.weixin.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.weixin.R;
import cn.itcast.weixin.activity.UserInfoActivity;
import cn.itcast.weixin.adapter.ContactAdapter;
import cn.itcast.weixin.bean.ContactBean;
import cn.itcast.weixin.db.RemarkDao;
import cn.itcast.weixin.utils.WeixinDataUtils;
import cn.itcast.weixin.view.SideIndexBar;

/**
 * 通讯录 Tab：功能入口 + 按字母分组的联系人列表 + 右侧字母索引栏。
 *
 * 备注是从数据库读的，所以每次回到这个页面（onResume）都会重新套一遍备注，
 * 在资料页改完备注返回，这里的名字立刻就是新的。
 */
public class ContactsFragment extends Fragment {

    private ListView listView;
    private ContactAdapter adapter;
    private TextView tvLetterHint;
    private final List<ContactBean> contacts = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideHint = new Runnable() {
        @Override
        public void run() {
            if (tvLetterHint != null) {
                tvLetterHint.setVisibility(View.GONE);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = view.findViewById(R.id.list_contacts);
        tvLetterHint = view.findViewById(R.id.tv_letter_hint);
        SideIndexBar indexBar = view.findViewById(R.id.index_bar);

        List<ContactAdapter.Entry> entries = new ArrayList<>();
        entries.add(new ContactAdapter.Entry(getString(R.string.new_friend), R.drawable.ic_new_friend));
        entries.add(new ContactAdapter.Entry(getString(R.string.group_chat), R.drawable.ic_group_chat));
        entries.add(new ContactAdapter.Entry(getString(R.string.tag), R.drawable.ic_tag));
        entries.add(new ContactAdapter.Entry(getString(R.string.mp), R.drawable.ic_mp));

        adapter = new ContactAdapter(getActivity(), entries);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Object item = adapter.getItem(position);
                if (item instanceof ContactAdapter.Entry) {
                    Toast.makeText(getActivity(), ((ContactAdapter.Entry) item).title,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (item instanceof ContactBean) {
                    ContactBean contact = (ContactBean) item;
                    Intent intent = new Intent(getActivity(), UserInfoActivity.class);
                    intent.putExtra("contactId", contact.getId());
                    intent.putExtra("name", contact.getName());
                    intent.putExtra("avatar", contact.getAvatar());
                    intent.putExtra("weixinId", contact.getWeixinId());
                    intent.putExtra("region", contact.getRegion());
                    startActivity(intent);
                }
            }
        });

        indexBar.setOnLetterChangedListener(new SideIndexBar.OnLetterChangedListener() {
            @Override
            public void onLetterChanged(String letter) {
                int position = adapter.getPositionForLetter(letter);
                if (position >= 0) {
                    listView.setSelection(position);
                }
                tvLetterHint.setText(letter);
                tvLetterHint.setVisibility(View.VISIBLE);
                handler.removeCallbacks(hideHint);
                handler.postDelayed(hideHint, 700);
            }
        });

        loadData();
    }

    private void loadData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String json = WeixinDataUtils.loadWeixinJson(getActivity());
                final List<ContactBean> loaded = WeixinDataUtils.parseContactList(json);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (adapter == null) {
                            return;
                        }
                        contacts.clear();
                        contacts.addAll(loaded);
                        applyRemarks();
                        adapter.setContacts(contacts);
                    }
                });
            }
        }).start();
    }

    /** 把数据库里的备注套到联系人的 remark 字段上 */
    private void applyRemarks() {
        for (ContactBean contact : contacts) {
            contact.setRemark(RemarkDao.getByName(contact.getName()));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 从资料页改完备注回来，这里重新套一遍
        if (adapter != null && !contacts.isEmpty()) {
            applyRemarks();
            adapter.setContacts(contacts);
        }
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(hideHint);
        super.onDestroyView();
    }
}
