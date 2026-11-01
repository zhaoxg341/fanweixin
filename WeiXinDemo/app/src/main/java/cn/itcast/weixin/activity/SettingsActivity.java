package cn.itcast.weixin.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cn.itcast.weixin.R;
import cn.itcast.weixin.utils.AppPrefs;
import cn.itcast.weixin.utils.ChatClient;

/**
 * 设置页（"我-设置"）：微信风格的条目列表。
 * 可改 昵称 / 微信号 / 性别 / 地区，也有服务器设置和退出登录。
 * 改完即时生效：个人卡片、聊天右侧气泡、局域网身份都读 AppPrefs。
 */
public class SettingsActivity extends AppCompatActivity {

    private TextView tvNickValue;
    private TextView tvWxidValue;
    private TextView tvGenderValue;
    private TextView tvRegionValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ((TextView) findViewById(R.id.tv_title)).setText(R.string.settings_title);
        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.iv_right).setVisibility(View.GONE);

        bindRow(R.id.row_nick, R.string.settings_nick);
        bindRow(R.id.row_wxid, R.string.settings_wxid);
        bindRow(R.id.row_gender, R.string.settings_gender);
        bindRow(R.id.row_region, R.string.settings_region);
        bindRow(R.id.row_server, R.string.settings_server);

        tvNickValue = findViewById(R.id.row_nick).findViewById(R.id.tv_value);
        tvWxidValue = findViewById(R.id.row_wxid).findViewById(R.id.tv_value);
        tvGenderValue = findViewById(R.id.row_gender).findViewById(R.id.tv_value);
        tvRegionValue = findViewById(R.id.row_region).findViewById(R.id.tv_value);

        findViewById(R.id.row_nick).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditNick();
            }
        });
        findViewById(R.id.row_wxid).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditWxId();
            }
        });
        findViewById(R.id.row_gender).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPickGender();
            }
        });
        findViewById(R.id.row_region).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditRegion();
            }
        });
        // 服务器设置沿用登录页的编辑模式
        findViewById(R.id.row_server).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                intent.putExtra("edit", true);
                startActivity(intent);
            }
        });
        findViewById(R.id.btn_logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshValues();
    }

    private void bindRow(int rowId, int labelRes) {
        ((TextView) findViewById(rowId).findViewById(R.id.tv_label)).setText(labelRes);
    }

    private void refreshValues() {
        tvNickValue.setText(AppPrefs.getMyNick());
        tvWxidValue.setText(AppPrefs.getMyWxId());
        String gender = AppPrefs.getMyGender();
        tvGenderValue.setText(gender.isEmpty()
                ? getString(R.string.settings_not_set) : gender);
        String region = AppPrefs.getMyRegion();
        tvRegionValue.setText(region.isEmpty()
                ? getString(R.string.settings_not_set) : region);
    }

    /** 通用单行输入弹窗 */
    private void showTextEditor(int titleRes, int hintRes, String current,
                                final OnSave saver) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_edit, null);
        final EditText input = content.findViewById(R.id.et_dialog_input);
        input.setHint(hintRes);
        input.setText(current);
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setView(content)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_confirm,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                saver.save(input.getText().toString().trim());
                            }
                        })
                .show();
    }

    private interface OnSave {
        void save(String value);
    }

    private void showEditNick() {
        showTextEditor(R.string.edit_nick_title, R.string.login_nick_hint,
                AppPrefs.getMyNick(), new OnSave() {
                    @Override
                    public void save(String value) {
                        if (value.isEmpty()) {
                            Toast.makeText(SettingsActivity.this, R.string.nick_empty,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        AppPrefs.setMyNick(value);
                        // 服务器按昵称认人，改名后重新登录一次
                        ChatClient.get().disconnect();
                        ChatClient.get().connectIfNeeded();
                        refreshValues();
                        Toast.makeText(SettingsActivity.this, R.string.nick_saved,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEditWxId() {
        showTextEditor(R.string.settings_wxid, R.string.settings_wxid_hint,
                AppPrefs.getMyWxId(), new OnSave() {
                    @Override
                    public void save(String value) {
                        if (value.isEmpty()) {
                            Toast.makeText(SettingsActivity.this,
                                    R.string.settings_wxid_invalid,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        AppPrefs.setMyWxId(value);
                        refreshValues();
                        Toast.makeText(SettingsActivity.this,
                                R.string.settings_wxid_saved, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showPickGender() {
        final String[] options = {
                getString(R.string.settings_gender_male),
                getString(R.string.settings_gender_female),
                getString(R.string.settings_gender_secret)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_gender)
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppPrefs.setMyGender(options[which]);
                        refreshValues();
                        Toast.makeText(SettingsActivity.this,
                                R.string.settings_gender_saved, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showEditRegion() {
        showTextEditor(R.string.settings_region, R.string.settings_region_hint,
                AppPrefs.getMyRegion(), new OnSave() {
                    @Override
                    public void save(String value) {
                        AppPrefs.setMyRegion(value);
                        refreshValues();
                        Toast.makeText(SettingsActivity.this,
                                R.string.settings_region_saved, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_logout)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_confirm,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AppPrefs.logout();
                                ChatClient.get().disconnect();
                                Toast.makeText(SettingsActivity.this, R.string.logout_done,
                                        Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SettingsActivity.this,
                                        LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }
                        })
                .show();
    }
}
