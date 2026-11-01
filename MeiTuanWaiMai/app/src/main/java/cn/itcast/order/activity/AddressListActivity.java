package cn.itcast.order.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import cn.itcast.order.R;
import cn.itcast.order.adapter.AddressAdapter;
import cn.itcast.order.bean.AddressBean;
import cn.itcast.order.utils.AppPrefs;

/**
 * 收货地址列表。
 * 从结算页进入（select_mode=true）时，点击地址直接把结果回传给结算页；
 * 从“我的”进入时，点击地址进入编辑。
 */
public class AddressListActivity extends AppCompatActivity {

    private ListView lvAddress;
    private TextView tvEmpty;
    private AddressAdapter adapter;

    private boolean selectMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_list);

        selectMode = getIntent().getBooleanExtra("select_mode", false);

        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(selectMode ? "选择收货地址" : "收货地址");

        lvAddress = findViewById(R.id.lv_address);
        tvEmpty = findViewById(R.id.tv_empty);

        adapter = new AddressAdapter(this, new AddressAdapter.OnEditListener() {
            @Override
            public void onEdit(AddressBean bean) {
                openEdit(bean);
            }
        });
        lvAddress.setAdapter(adapter);
        lvAddress.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AddressBean bean = (AddressBean) adapter.getItem(position);
                if (selectMode) {
                    // 选中即设为默认，下次结算默认使用
                    AppPrefs.setDefaultAddress(AddressListActivity.this, bean.getId());
                    Intent data = new Intent();
                    data.putExtra("address", bean);
                    setResult(RESULT_OK, data);
                    finish();
                } else {
                    openEdit(bean);
                }
            }
        });

        findViewById(R.id.tv_add_address).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AddressListActivity.this, AddressEditActivity.class));
            }
        });
    }

    private void openEdit(AddressBean bean) {
        Intent intent = new Intent(this, AddressEditActivity.class);
        intent.putExtra("address", bean);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<AddressBean> list = AppPrefs.getAddresses(this);
        adapter.setData(list);
        boolean empty = list.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        lvAddress.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
