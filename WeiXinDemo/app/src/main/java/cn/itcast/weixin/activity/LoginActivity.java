package cn.itcast.weixin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cn.itcast.weixin.R;
import cn.itcast.weixin.utils.AppPrefs;
import cn.itcast.weixin.utils.ChatClient;

/**
 * 登录页（同时也是「修改昵称 / 服务器」的设置页）。
 *
 * 需要填的东西只有两样：你的昵称、要不要连服务器。
 * 昵称决定了对方看到的名字，也是服务器转发消息时的"收件人"；
 * 不勾选"连服务器"就是用本地演示数据，功能和第一版完全一样。
 *
 * 从「我 → 设置」以 edit=true 进来时不跳转，保存后直接返回。
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etNick;
    private EditText etHost;
    private EditText etPort;
    private CheckBox cbLan;

    private boolean editMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        editMode = getIntent().getBooleanExtra("edit", false);
        // 已经登录过、又不是进来改设置的，直接进主界面，不让用户每次都填一遍
        if (!editMode && AppPrefs.isLoggedIn()) {
            goMain();
            return;
        }

        setContentView(R.layout.activity_login);
        etNick = findViewById(R.id.et_nick);
        etHost = findViewById(R.id.et_host);
        etPort = findViewById(R.id.et_port);
        cbLan = findViewById(R.id.cb_lan);

        // 预填上一次的设置
        etNick.setText(AppPrefs.getMyNick());
        etNick.setSelection(etNick.getText().length());
        etHost.setText(AppPrefs.getServerHost());
        etPort.setText(String.valueOf(AppPrefs.getServerPort()));
        cbLan.setChecked(AppPrefs.isLanMode());
        updateHostEnabled();

        cbLan.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateHostEnabled();
            }
        });

        TextView btnLogin = findViewById(R.id.btn_login);
        btnLogin.setText(editMode ? R.string.login_save : R.string.login_enter);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSave();
            }
        });
    }

    /** 不连服务器时把地址、端口置灰，免得让人以为必须填 */
    private void updateHostEnabled() {
        boolean lan = cbLan.isChecked();
        etHost.setEnabled(lan);
        etPort.setEnabled(lan);
        etHost.setAlpha(lan ? 1f : 0.45f);
        etPort.setAlpha(lan ? 1f : 0.45f);
    }

    private void doSave() {
        String nick = etNick.getText().toString().trim();
        if (nick.isEmpty()) {
            Toast.makeText(this, R.string.login_nick_required, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean lan = cbLan.isChecked();
        String host = etHost.getText().toString().trim();
        int port = AppPrefs.DEFAULT_PORT;
        if (lan) {
            if (host.isEmpty()) {
                Toast.makeText(this, R.string.login_host_required, Toast.LENGTH_SHORT).show();
                return;
            }
            String portText = etPort.getText().toString().trim();
            if (!portText.isEmpty()) {
                try {
                    port = Integer.parseInt(portText);
                } catch (NumberFormatException ignored) {
                    // 填的不是数字就用默认端口
                }
            }
        }

        AppPrefs.setMyNick(nick);
        AppPrefs.setLanMode(lan);
        if (lan) {
            AppPrefs.setServerHost(host);
            AppPrefs.setServerPort(port);
        }
        AppPrefs.setLoggedIn(true);

        // 昵称或服务器地址可能变了，按新配置重连一次
        ChatClient.get().disconnect();
        ChatClient.get().connectIfNeeded();

        if (editMode) {
            Toast.makeText(this, R.string.nick_saved, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            goMain();
        }
    }

    private void goMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
