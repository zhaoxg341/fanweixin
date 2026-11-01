package cn.itcast.milktea.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.itcast.milktea.R;
import cn.itcast.milktea.utils.AppPrefs;
import cn.itcast.milktea.utils.GoodsDataUtils;

/**
 * "我的"页：头像昵称（点击可改昵称）+ 会员卡 + 优惠券/地址/客服/关于。
 */
public class MineFragment extends Fragment {

    private TextView tvNick;
    private TextView tvSub;
    private ImageView ivAvatar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine, container, false);

        tvNick = view.findViewById(R.id.tv_mine_nick);
        tvSub = view.findViewById(R.id.tv_mine_sub);
        ivAvatar = view.findViewById(R.id.iv_mine_avatar);
        ivAvatar.setBackgroundResource(R.drawable.bg_circle_primary);
        ivAvatar.setClipToOutline(true);

        view.findViewById(R.id.ll_member_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "会员储值功能为课设演示，暂未开放",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // 点头部改昵称
        View header = (View) tvNick.getParent().getParent();
        header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditNickDialog();
            }
        });

        view.findViewById(R.id.row_coupon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCouponDialog();
            }
        });
        view.findViewById(R.id.row_address).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditAddressDialog();
            }
        });
        view.findViewById(R.id.row_service).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getActivity())
                        .setTitle("客服中心")
                        .setMessage("营业时间 09:00-22:30\n客服电话：400-888-8888\n课设演示项目，暂不支持在线转人工。")
                        .setPositiveButton("知道了", null)
                        .show();
            }
        });
        view.findViewById(R.id.row_about).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getActivity())
                        .setTitle("关于茶语时光")
                        .setMessage("茶语时光 v1.0\n\n"
                                + "一个用 Java + Android 原生控件实现的奶茶点单 Demo：\n"
                                + "· 商品数据来自 assets/milktea.json\n"
                                + "· 购物车用静态单例管理\n"
                                + "· 订单用 SQLite 持久化\n"
                                + "· 支付流程为本地模拟")
                        .setPositiveButton("好的", null)
                        .show();
            }
        });

        refreshProfile();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshProfile();
    }

    private void refreshProfile() {
        if (getActivity() == null || tvNick == null) {
            return;
        }
        tvNick.setText(AppPrefs.getNick(getActivity()));
        tvSub.setText(AppPrefs.getPhone(getActivity()) + " · 积分 128");
        int resId = GoodsDataUtils.getDrawableId(getActivity(), "drink2");
        if (resId != 0) {
            ivAvatar.setImageResource(resId);
        }
    }

    /** 改昵称：弹一个带 EditText 的对话框 */
    private void showEditNickDialog() {
        final EditText input = new EditText(getActivity());
        input.setSingleLine(true);
        input.setText(AppPrefs.getNick(getActivity()));
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(getActivity())
                .setTitle("修改昵称")
                .setView(input)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String nick = input.getText().toString().trim();
                        if (nick.isEmpty()) {
                            Toast.makeText(getActivity(), "昵称不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        AppPrefs.setNick(getActivity(), nick);
                        refreshProfile();
                    }
                })
                .show();
    }

    /** 改收货地址：外送时 ConfirmOrderActivity 会读这里 */
    private void showEditAddressDialog() {
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(AppPrefs.getAddress(getActivity()));
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(getActivity())
                .setTitle("修改收货地址")
                .setView(input)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String address = input.getText().toString().trim();
                        if (address.isEmpty()) {
                            Toast.makeText(getActivity(), "地址不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        AppPrefs.setAddress(getActivity(), address);
                        Toast.makeText(getActivity(), "地址已保存", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showCouponDialog() {
        if (getActivity() == null) {
            return;
        }
        new AlertDialog.Builder(getActivity())
                .setTitle("我的优惠券")
                .setMessage("满 25 减 4\n满 40 减 10\n\n"
                        + "结算时会自动使用最优惠的一档，无需手动选择。")
                .setPositiveButton("知道了", null)
                .show();
    }
}
