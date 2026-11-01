package cn.itcast.weixin.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.itcast.weixin.R;
import cn.itcast.weixin.activity.PhotoViewerActivity;
import cn.itcast.weixin.bean.MsgBean;
import cn.itcast.weixin.db.AvatarDao;
import cn.itcast.weixin.db.RemarkDao;
import cn.itcast.weixin.utils.AvatarStore;
import cn.itcast.weixin.utils.MomentPhotoStore;
import cn.itcast.weixin.view.CircleImageView;

/**
 * 聊天消息适配器。行样式：
 * 0 时间标签   1 左侧消息（对方）   2 右侧消息（我）   3 撤回提示（灰色居中一行）
 * 一条消息被撤回后，整行降级显示成第 3 种。
 * 时间与上一条不同时，会自动插入一行时间标签。
 */
public class MsgAdapter extends BaseAdapter {

    private static final int TYPE_TIME = 0;
    private static final int TYPE_LEFT = 1;
    private static final int TYPE_RIGHT = 2;
    private static final int TYPE_RECALLED = 3;

    /** 气泡上的交互都回调给聊天页处理（拆红包、收款、长按菜单、双击头像拍一拍） */
    public interface OnMsgAction {
        void onRedPacketClick(MsgBean msg);

        void onTransferClick(MsgBean msg);

        /** 双击头像（拍一拍）。fromMe=true 是双击自己的头像 */
        void onAvatarDoubleTap(MsgBean msg);

        boolean onMsgLongClick(MsgBean msg);
    }

    private final Context context;
    /** 对方的昵称，用来查有没有自定义头像 */
    private final String otherName;
    /** assets 里的原头像，没自定义时用它 */
    private String otherAvatar;
    /** 我的头像：可能在内置图和相册照片之间切换，所以要能更新 */
    private String myAvatar;
    private final List<Object> rows = new ArrayList<>();
    private OnMsgAction actionListener;

    public MsgAdapter(Context context, String otherName, String otherAvatar, String myAvatar) {
        this.context = context;
        this.otherName = otherName;
        this.otherAvatar = otherAvatar;
        this.myAvatar = myAvatar;
    }

    public void setActionListener(OnMsgAction listener) {
        this.actionListener = listener;
    }

    /** 对方头像被改了，聊天页重画一遍 */
    public void setOtherAvatar(String otherAvatar) {
        this.otherAvatar = otherAvatar;
        notifyDataSetChanged();
    }

    /** 我在"我"页面换了头像，回到聊天页时同步右侧气泡 */
    public void setMyAvatar(String myAvatar) {
        this.myAvatar = myAvatar;
        notifyDataSetChanged();
    }

    public void setData(List<MsgBean> messages) {
        rows.clear();
        if (messages != null) {
            String lastTime = null;
            for (MsgBean msg : messages) {
                if (msg.getTime() != null && !msg.getTime().equals(lastTime)) {
                    rows.add(msg.getTime());
                    lastTime = msg.getTime();
                }
                rows.add(msg);
            }
        }
        notifyDataSetChanged();
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
        return 4;
    }

    @Override
    public int getItemViewType(int position) {
        Object o = rows.get(position);
        if (o instanceof String) {
            return TYPE_TIME;
        }
        MsgBean msg = (MsgBean) o;
        if (msg.isRecalled() || msg.isPat()) {
            return TYPE_RECALLED;
        }
        return msg.isFromMe() ? TYPE_RIGHT : TYPE_LEFT;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int type = getItemViewType(position);

        if (type == TYPE_TIME || type == TYPE_RECALLED) {
            TextView tv = (TextView) convertView;
            if (tv == null) {
                tv = (TextView) LayoutInflater.from(context)
                        .inflate(R.layout.item_msg_time, parent, false);
            }
            if (type == TYPE_TIME) {
                tv.setText((String) rows.get(position));
            } else {
                MsgBean msg = (MsgBean) rows.get(position);
                if (msg.isPat()) {
                    // 拍一拍：直接显示提示文字（"你拍了拍×××" / "××× 拍了拍你"）
                    tv.setText(msg.getContent());
                } else {
                    tv.setText(msg.isFromMe()
                            ? context.getString(R.string.msg_recalled_by_me)
                            : context.getString(R.string.msg_recalled_by_other,
                                    RemarkDao.displayName(otherName)));
                }
            }
            return tv;
        }

        int layoutId = type == TYPE_RIGHT ? R.layout.item_msg_right : R.layout.item_msg_left;
        Holder holder;
        if (convertView == null || !(convertView.getTag() instanceof Holder)) {
            convertView = LayoutInflater.from(context).inflate(layoutId, parent, false);
            holder = new Holder();
            holder.ivAvatar = convertView.findViewById(R.id.iv_avatar);
            holder.tvMsg = convertView.findViewById(R.id.tv_msg);
            holder.ivPicture = convertView.findViewById(R.id.iv_picture);
            holder.redCard = convertView.findViewById(R.id.red_card);
            holder.tvRpGreeting = convertView.findViewById(R.id.tv_rp_greeting);
            holder.tvRpState = convertView.findViewById(R.id.tv_rp_state);
            holder.transferCard = convertView.findViewById(R.id.transfer_card);
            holder.tvTfAmount = convertView.findViewById(R.id.tv_tf_amount);
            holder.tvTfNote = convertView.findViewById(R.id.tv_tf_note);
            holder.tvTfState = convertView.findViewById(R.id.tv_tf_state);
            holder.llVoice = convertView.findViewById(R.id.ll_voice);
            holder.ivVoiceIcon = convertView.findViewById(R.id.iv_voice_icon);
            holder.tvVoiceDur = convertView.findViewById(R.id.tv_voice_dur);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        final MsgBean msg = (MsgBean) rows.get(position);
        holder.redCard.setVisibility(View.GONE);
        holder.transferCard.setVisibility(View.GONE);
        holder.llVoice.setVisibility(View.GONE);
        holder.ivPicture.setVisibility(View.GONE);
        holder.ivPicture.setOnClickListener(null);

        if (msg.isRedPacket()) {
            bindRedPacket(holder, msg);
        } else if (msg.isTransfer()) {
            bindTransfer(holder, msg);
        } else if (msg.isVoice()) {
            bindVoice(holder, msg);
        } else if (msg.isImage()) {
            bindImage(holder, msg.getContent());
        } else {
            holder.tvMsg.setVisibility(View.VISIBLE);
            holder.tvMsg.setText(msg.getContent());
        }

        String avatar = type == TYPE_RIGHT ? myAvatar
                : AvatarDao.displayAvatar(otherName, otherAvatar);
        AvatarStore.apply(holder.ivAvatar, avatar, R.drawable.avatar_me);

        // 双击头像 = 拍一拍；单击不做别的（真微信也是双击才触发）
        holder.ivAvatar.setOnClickListener(new View.OnClickListener() {
            private long lastTap = 0;

            @Override
            public void onClick(View v) {
                long now = System.currentTimeMillis();
                if (now - lastTap < 350) {
                    lastTap = 0;
                    if (actionListener != null) {
                        actionListener.onAvatarDoubleTap(msg);
                    }
                } else {
                    lastTap = now;
                }
            }
        });

        // 长按任意气泡弹菜单（复制 / 撤回 / 删除），红包和语音也一样
        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return actionListener != null && actionListener.onMsgLongClick(msg);
            }
        });
        return convertView;
    }

    /** 红包消息：橙色卡片，留言 + 领取状态，点一下拆红包 */
    private void bindRedPacket(Holder holder, final MsgBean msg) {
        holder.tvMsg.setVisibility(View.GONE);
        holder.redCard.setVisibility(View.VISIBLE);
        holder.tvRpGreeting.setText(msg.redPacketGreeting());
        holder.tvRpState.setText(msg.redPacketOpened()
                ? context.getString(R.string.rp_state_done)
                : context.getString(R.string.rp_state_wait));
        holder.redCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null) {
                    actionListener.onRedPacketClick(msg);
                }
            }
        });
    }

    /** 转账消息：橙色卡片，金额 + 留言 + 收款状态，点一下收款/查看 */
    private void bindTransfer(Holder holder, final MsgBean msg) {
        holder.tvMsg.setVisibility(View.GONE);
        holder.transferCard.setVisibility(View.VISIBLE);
        holder.tvTfAmount.setText("¥" + msg.transferAmount());
        String note = msg.transferNote();
        holder.tvTfNote.setText(note == null || note.isEmpty()
                ? context.getString(R.string.transfer_default_note) : note);
        holder.tvTfState.setText(msg.transferReceived()
                ? context.getString(R.string.transfer_state_done)
                : context.getString(R.string.transfer_state_wait));
        holder.transferCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null) {
                    actionListener.onTransferClick(msg);
                }
            }
        });
    }

    /** 语音消息：喇叭 + 秒数，点一下播放动画（演示数据没有真的录音） */
    private void bindVoice(Holder holder, final MsgBean msg) {
        holder.tvMsg.setVisibility(View.GONE);
        holder.llVoice.setVisibility(View.VISIBLE);
        holder.tvVoiceDur.setText(context.getString(R.string.voice_seconds, msg.voiceSeconds()));
        holder.ivVoiceIcon.clearAnimation();
        holder.llVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playVoiceAnimation(holder.ivVoiceIcon);
            }
        });
    }

    /** 播放动画：喇叭缩放抖三下，模拟「正在播放」 */
    private void playVoiceAnimation(final ImageView icon) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(icon, "scaleX", 1f, 1.35f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(icon, "scaleY", 1f, 1.35f, 1f);
        scaleX.setRepeatCount(2);
        scaleY.setRepeatCount(2);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(320);
        set.start();
    }

    /** 图片消息：直接显示缩略图，点一下进全屏查看器 */
    private void bindImage(Holder holder, final String path) {
        holder.tvMsg.setVisibility(View.GONE);
        holder.ivPicture.setVisibility(View.VISIBLE);

        Bitmap bitmap = MomentPhotoStore.load(path);
        if (bitmap != null) {
            holder.ivPicture.setImageBitmap(bitmap);
        } else {
            holder.ivPicture.setImageResource(R.drawable.avatar_default);
        }
        holder.ivPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PhotoViewerActivity.class);
                intent.putStringArrayListExtra(PhotoViewerActivity.EXTRA_IMAGES,
                        new ArrayList<>(Collections.singletonList(path)));
                context.startActivity(intent);
            }
        });
    }

    static class Holder {
        CircleImageView ivAvatar;
        TextView tvMsg;
        ImageView ivPicture;
        LinearLayout redCard;
        TextView tvRpGreeting;
        TextView tvRpState;
        LinearLayout transferCard;
        TextView tvTfAmount;
        TextView tvTfNote;
        TextView tvTfState;
        LinearLayout llVoice;
        ImageView ivVoiceIcon;
        TextView tvVoiceDur;
    }
}
