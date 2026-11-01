package cn.itcast.weixin.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.weixin.R;
import cn.itcast.weixin.activity.GameLobbyActivity;
import cn.itcast.weixin.activity.MomentsActivity;
import cn.itcast.weixin.adapter.DiscoverAdapter;

/**
 * 发现 Tab：分组功能列表（数据是固定的，直接写在代码里）
 */
public class DiscoverFragment extends Fragment {

    private DiscoverAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discover, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView listView = view.findViewById(R.id.list_discover);
        adapter = new DiscoverAdapter(getActivity());
        listView.setAdapter(adapter);
        adapter.setGroups(buildGroups());

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Object item = adapter.getItem(position);
                if (!(item instanceof DiscoverAdapter.Item)) {
                    return;
                }
                DiscoverAdapter.Item data = (DiscoverAdapter.Item) item;
                if (getString(R.string.moments).equals(data.title)) {
                    startActivity(new Intent(getActivity(), MomentsActivity.class));
                } else if (getString(R.string.game).equals(data.title)) {
                    startActivity(new Intent(getActivity(), GameLobbyActivity.class));
                } else {
                    Toast.makeText(getActivity(), data.title, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /** 五个分组，组之间会自动插入灰色间隔 */
    private List<List<DiscoverAdapter.Item>> buildGroups() {
        List<List<DiscoverAdapter.Item>> groups = new ArrayList<>();

        List<DiscoverAdapter.Item> g1 = new ArrayList<>();
        g1.add(new DiscoverAdapter.Item(getString(R.string.moments), R.drawable.ic_moments));
        groups.add(g1);

        List<DiscoverAdapter.Item> g2 = new ArrayList<>();
        g2.add(new DiscoverAdapter.Item(getString(R.string.channels), R.drawable.ic_channels));
        groups.add(g2);

        List<DiscoverAdapter.Item> g3 = new ArrayList<>();
        g3.add(new DiscoverAdapter.Item(getString(R.string.scan), R.drawable.ic_scan));
        g3.add(new DiscoverAdapter.Item(getString(R.string.shake), R.drawable.ic_shake));
        groups.add(g3);

        List<DiscoverAdapter.Item> g4 = new ArrayList<>();
        g4.add(new DiscoverAdapter.Item(getString(R.string.look), R.drawable.ic_look));
        g4.add(new DiscoverAdapter.Item(getString(R.string.search_discover), R.drawable.ic_search_blue));
        groups.add(g4);

        List<DiscoverAdapter.Item> g5 = new ArrayList<>();
        g5.add(new DiscoverAdapter.Item(getString(R.string.nearby), R.drawable.ic_nearby));
        g5.add(new DiscoverAdapter.Item(getString(R.string.shop), R.drawable.ic_shop));
        g5.add(new DiscoverAdapter.Item(getString(R.string.game), R.drawable.ic_game));
        groups.add(g5);

        return groups;
    }
}
