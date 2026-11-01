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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import cn.itcast.weixin.R;
import cn.itcast.weixin.activity.ChatActivity;
import cn.itcast.weixin.activity.MainActivity;
import cn.itcast.weixin.adapter.ChatAdapter;
import cn.itcast.weixin.bean.ChatBean;
import cn.itcast.weixin.utils.ChatManager;
import cn.itcast.weixin.utils.WeixinDataUtils;

/**
 * 微信 Tab：会话列表。
 *
 * 数据统一放在 ChatManager 里，并且注册了监听器：
 * 在线模式下对方发来消息时，这个列表不用手动刷新就会自己更新，
 * 底部的未读总数红点也会跟着变。
 */
public class ChatsFragment extends Fragment {

    private ListView listView;
    private ChatAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /** 任何会话有变化（自己发的、对方发来的、清未读）都重新刷一遍 */
    private final ChatManager.OnChatChangedListener chatListener =
            new ChatManager.OnChatChangedListener() {
                @Override
                public void onChatChanged(int chatId) {
                    refresh();
                }
            };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = view.findViewById(R.id.list_chats);
        adapter = new ChatAdapter(getActivity());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                ChatBean chat = (ChatBean) adapter.getItem(position);
                ChatManager.clearUnread(chat.getId());
                refresh();
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("chatId", chat.getId());
                intent.putExtra("chatName", chat.getName());
                intent.putExtra("chatAvatar", chat.getAvatar());
                startActivity(intent);
            }
        });
        loadData();
        ChatManager.addListener(chatListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 从聊天页返回、发送过消息时，这里刷新最后一条消息与红点
        refresh();
    }

    @Override
    public void onDestroyView() {
        ChatManager.removeListener(chatListener);
        super.onDestroyView();
    }

    /** 首次进入读一次 JSON，之后复用 ChatManager 里的数据 */
    private void loadData() {
        if (ChatManager.isInited()) {
            refresh();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String json = WeixinDataUtils.loadWeixinJson(getActivity());
                final List<ChatBean> chats = WeixinDataUtils.parseChatList(json);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ChatManager.init(chats);
                        refresh();
                    }
                });
            }
        }).start();
    }

    /** 刷新列表并同步底部 Tab 红点 */
    public void refresh() {
        if (adapter != null) {
            adapter.setData(ChatManager.getChats());
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateChatBadge();
        }
    }
}
