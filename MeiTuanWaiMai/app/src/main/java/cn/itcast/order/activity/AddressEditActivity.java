package cn.itcast.order.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import cn.itcast.order.R;
import cn.itcast.order.bean.AddressBean;
import cn.itcast.order.utils.AppPrefs;

/**
 * 新增 / 编辑收货地址
 */
public class AddressEditActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etPhone;
    private EditText etDetail;
    private CheckBox cbDefault;

    /** 为空表示新增 */
    private AddressBean editing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_edit);

        editing = (AddressBean) getIntent().getSerializableExtra("address");

        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(editing == null ? "新增收货地址" : "编辑收货地址");

        etName = findViewById(R.id.et_name);
        etPhone = findViewById(R.id.et_phone);
        etDetail = findViewById(R.id.et_detail);
        cbDefault = findViewById(R.id.cb_default);

        TextView tvDelete = findViewById(R.id.tv_delete);
        if (editing != null) {
            etName.setText(editing.getName());
            etPhone.setText(editing.getPhone());
            etDetail.setText(editing.getDetail());
            cbDefault.setChecked(editing.isDefault());
            tvDelete.setVisibility(View.VISIBLE);
            tvDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDelete();
                }
            });
        }

        findViewById(R.id.tv_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
    }

    private void save() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String detail = etDetail.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "请填写联系人", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone.replace(" ", "").length() < 7) {
            Toast.makeText(this, "请填写正确的手机号", Toast.LENGTH_SHORT).show();
            return;
        }
        if (detail.isEmpty()) {
            Toast.makeText(this, "请填写详细地址", Toast.LENGTH_SHORT).show();
            return;
        }

        List<AddressBean> list = AppPrefs.getAddresses(this);
        boolean makeDefault = cbDefault.isChecked() || list.isEmpty();

        AddressBean target = editing;
        if (target == null) {
            target = new AddressBean();
            target.setId(System.currentTimeMillis());
            list.add(target);
        }
        target.setName(name);
        target.setPhone(phone);
        target.setDetail(detail);
        target.setDefault(makeDefault);

        if (makeDefault) {
            for (AddressBean bean : list) {
                if (bean.getId() != target.getId()) {
                    bean.setDefault(false);
                }
            }
        }
        AppPrefs.saveAddresses(this, list);

        Toast.makeText(this, "地址已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setMessage("确定删除这个收货地址吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        delete();
                    }
                })
                .show();
    }

    private void delete() {
        List<AddressBean> list = AppPrefs.getAddresses(this);
        AddressBean removed = null;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == editing.getId()) {
                removed = list.remove(i);
                break;
            }
        }
        // 删掉的是默认地址时，把第一个地址设为默认
        if (removed != null && removed.isDefault() && !list.isEmpty()) {
            list.get(0).setDefault(true);
        }
        AppPrefs.saveAddresses(this, list);
        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
        finish();
    }
}
