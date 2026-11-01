package cn.itcast.milktea.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import cn.itcast.milktea.R;
import cn.itcast.milktea.activity.CartActivity;
import cn.itcast.milktea.activity.MainActivity;
import cn.itcast.milktea.activity.OrderDetailActivity;
import cn.itcast.milktea.activity.PayActivity;
import cn.itcast.milktea.adapter.OrderAdapter;
import cn.itcast.milktea.bean.OrderBean;
import cn.itcast.milktea.db.OrderDao;
import cn.itcast.milktea.utils.CartManager;

/**
 * 订单列表页。
 *
 * 每次 onResume 都重新查一次数据库：支付、确认取餐、取消订单都会改状态，
 * 从别的页面回来必须拿最新数据，不能只在 onCreate 查一次。
 */
public class OrdersFragment extends Fragment implements OrderAdapter.OnOrderActionListener {

    private OrderAdapter adapter;
    private ListView lvOrders;
    private View emptyView;
    private OrderDao orderDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        TextView title = view.findViewById(R.id.tv_title);
        title.setText(R.string.title_orders);
        view.findViewById(R.id.iv_back).setVisibility(View.INVISIBLE);

        lvOrders = view.findViewById(R.id.lv_orders);
        emptyView = view.findViewById(R.id.ll_orders_empty);
        adapter = new OrderAdapter(getActivity(), this);
        lvOrders.setAdapter(adapter);

        view.findViewById(R.id.tv_orders_go).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).selectTab(MainActivity.TAB_HOME);
                }
            }
        });

        orderDao = new OrderDao(getActivity());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        if (getActivity() == null) {
            return;
        }
        List<OrderBean> orders = orderDao.queryAll();
        adapter.setData(orders);
        emptyView.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
        lvOrders.setVisibility(orders.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ---------------- 卡片回调 ----------------

    @Override
    public void onOrderClick(OrderBean order) {
        openDetail(order);
    }

    /** 左边按钮：待支付→取消订单；其它→查看详情 */
    @Override
    public void onLeftAction(OrderBean order) {
        if (order.getStatus() == OrderBean.STATUS_UNPAID) {
            confirmCancel(order);
        } else {
            openDetail(order);
        }
    }

    /** 右边按钮：去支付 / 确认取餐 / 再来一单 */
    @Override
    public void onRightAction(OrderBean order) {
        if (order.getStatus() == OrderBean.STATUS_UNPAID) {
            Intent intent = new Intent(getActivity(), PayActivity.class);
            intent.putExtra("orderNo", order.getOrderNo());
            startActivity(intent);
        } else if (order.getStatus() == OrderBean.STATUS_MAKING) {
            confirmPickup(order);
        } else {
            reorder(order);
        }
    }

    private void openDetail(OrderBean order) {
        Intent intent = new Intent(getActivity(), OrderDetailActivity.class);
        intent.putExtra("orderNo", order.getOrderNo());
        startActivity(intent);
    }

    private void confirmCancel(final OrderBean order) {
        new AlertDialog.Builder(getActivity())
                .setTitle("取消订单")
                .setMessage("确定要取消这笔订单吗？")
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        orderDao.updateStatus(order.getOrderNo(), OrderBean.STATUS_CANCELED);
                        loadData();
                        Toast.makeText(getActivity(), "订单已取消", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void confirmPickup(final OrderBean order) {
        new AlertDialog.Builder(getActivity())
                .setTitle("确认取餐")
                .setMessage("确认已经拿到奶茶了吗？")
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        orderDao.updateStatus(order.getOrderNo(), OrderBean.STATUS_DONE);
                        loadData();
                        Toast.makeText(getActivity(), "感谢光临，期待下次再见～",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    /** 再来一单：按原规格重新加入购物车，然后跳到购物车 */
    private void reorder(OrderBean order) {
        CartManager.reorder(getActivity(), order);
        Toast.makeText(getActivity(), "已加入购物车", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(getActivity(), CartActivity.class));
    }
}
