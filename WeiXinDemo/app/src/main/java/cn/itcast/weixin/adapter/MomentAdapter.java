package cn.itcast.weixin.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.weixin.R;
import cn.itcast.weixin.bean.MomentBean;
import cn.itcast.weixin.db.MomentDao;
import cn.itcast.weixin.utils.AppPrefs;
import cn.itcast.weixin.utils.MomentPhotoStore;
import cn.itcast.weixin.utils.WeixinDataUtils;
import cn.itcast.weixin.view.CircleImageView;
import cn.itcast.weixin.view.FlowLayout;

/**
 * 朋友圈动态适配器。
 *
 * 图片有两种来源：
 * 1. assets 自带的示例图，JSON 里写的是文件名（moment1.jpg），走 getDrawableId；
 * 2. 用户从相册发的，存的是绝对路径，走 MomentPhotoStore 解码。
 * 两者在界面上没有区别，统一由 loadImage() 判断。
 *
 * 评论不是一整段文字，而是一条评论一个 TextView：
 * 这样点某条评论才能精确知道「我要回复谁」，长按也能定位到要删的那条。
 */
public class MomentAdapter extends BaseAdapter {

    /** 条目上的各种点击回调，交给 MomentsActivity 处理 */
    public interface OnMomentActionListener {
        void onLike(MomentBean moment);

        /** replyTo 为 null 表示直接评论动态，否则是回复某条评论 */
        void onComment(MomentBean moment, MomentBean.CommentBean replyTo);

        void onDeleteComment(MomentBean moment, MomentBean.CommentBean comment);

        void onImageClick(MomentBean moment, int index);

        void onLongPress(MomentBean moment);
    }

    private final Context context;
    private List<MomentBean> momentList = new ArrayList<>();
    private OnMomentActionListener listener;

    /** 九宫格单张图片的边长（px），构造时按屏幕宽度算一次 */
    private final int gridSize;
    private final int singleSize;

    public MomentAdapter(Context context) {
        this.context = context;
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        // 屏幕宽 - 左右 padding(24) - 头像(42) - 头像右边距(10) - 两道 6dp 间隙
        int available = screenWidth - dp(24 + 42 + 10 + 12);
        gridSize = Math.max(dp(64), available / 3);
        singleSize = Math.min(dp(200), available);
    }

    public void setListener(OnMomentActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<MomentBean> momentList) {
        this.momentList = momentList == null ? new ArrayList<MomentBean>() : momentList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return momentList.size();
    }

    @Override
    public Object getItem(int position) {
        return momentList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_moment, parent, false);
            holder = new ViewHolder();
            holder.ivAvatar = convertView.findViewById(R.id.iv_avatar);
            holder.tvName = convertView.findViewById(R.id.tv_name);
            holder.tvMineTag = convertView.findViewById(R.id.tv_mine_tag);
            holder.tvContent = convertView.findViewById(R.id.tv_content);
            holder.llImages = convertView.findViewById(R.id.ll_images);
            holder.tvTime = convertView.findViewById(R.id.tv_time);
            holder.btnComment = convertView.findViewById(R.id.btn_comment);
            holder.btnLike = convertView.findViewById(R.id.btn_like);
            holder.ivLike = convertView.findViewById(R.id.iv_like);
            holder.tvLike = convertView.findViewById(R.id.tv_like);
            holder.llLikeComment = convertView.findViewById(R.id.ll_like_comment);
            holder.tvLikes = convertView.findViewById(R.id.tv_likes);
            holder.vDivider = convertView.findViewById(R.id.v_divider);
            holder.llComments = convertView.findViewById(R.id.ll_comments);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final MomentBean moment = momentList.get(position);
        holder.tvName.setText(moment.getName());
        holder.tvContent.setText(moment.getContent());
        holder.tvContent.setVisibility(isEmpty(moment.getContent()) ? View.GONE : View.VISIBLE);
        holder.tvMineTag.setVisibility(moment.isMine() ? View.VISIBLE : View.GONE);
        // 时间按时间戳现算，页面放久了「几分钟前」会自己往后走
        holder.tvTime.setText(MomentDao.formatTime(moment.getTs()));

        bindAvatar(holder.ivAvatar, moment);
        bindImages(holder.llImages, moment);
        bindActions(holder, moment);
        bindLikeAndComment(holder, moment);

        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (listener != null) {
                    listener.onLongPress(moment);
                }
                return true;
            }
        });
        return convertView;
    }

    private void bindAvatar(CircleImageView view, MomentBean moment) {
        String avatar = moment.getAvatar();
        int fallback = WeixinDataUtils.getDrawableId(context, avatar);
        if (fallback == 0) {
            fallback = R.drawable.avatar_default;
        }
        // 自己发的动态直接用「我」当前的头像，换了头像朋友圈也跟着换
        if (moment.isMine()) {
            String mine = AppPrefs.getMyAvatar();
            int mineRes = WeixinDataUtils.getDrawableId(context, mine);
            if (mineRes != 0) {
                cn.itcast.weixin.utils.AvatarStore.apply(view, mine, mineRes);
            } else {
                cn.itcast.weixin.utils.AvatarStore.apply(view, mine, fallback);
            }
            return;
        }
        cn.itcast.weixin.utils.AvatarStore.apply(view, avatar, fallback);
    }

    /** 九宫格：一张时给大图，两张及以上统一小方块，一行最多 3 张 */
    private void bindImages(FlowLayout container, MomentBean moment) {
        container.removeAllViews();
        List<String> images = moment.getImages();
        if (images == null || images.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }
        container.setVisibility(View.VISIBLE);
        container.setSpacing(dp(6), dp(6));

        int count = Math.min(images.size(), 9);
        boolean single = count == 1;
        int size = single ? singleSize : gridSize;

        for (int i = 0; i < count; i++) {
            final int index = i;
            ImageView iv = new ImageView(context);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(size, size);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackgroundColor(0xFFF2F2F2);
            loadImage(iv, images.get(i));
            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onImageClick(moment, index);
                    }
                }
            });
            container.addView(iv);
        }
    }

    private void loadImage(ImageView view, String path) {
        if (MomentPhotoStore.isLocalFile(path)) {
            Bitmap bitmap = MomentPhotoStore.load(path);
            if (bitmap != null) {
                view.setImageBitmap(bitmap);
                return;
            }
            view.setImageResource(R.drawable.avatar_default);
            return;
        }
        int resId = WeixinDataUtils.getDrawableId(context, path);
        if (resId != 0) {
            view.setImageResource(resId);
        } else {
            view.setImageResource(R.drawable.avatar_default);
        }
    }

    /** 点赞 / 评论按钮，以及「我有没有点过赞」的图标状态 */
    private void bindActions(ViewHolder holder, final MomentBean moment) {
        final boolean liked = moment.isLikedByMe();
        holder.ivLike.setImageResource(liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        holder.tvLike.setText(liked ? R.string.unlike : R.string.like);
        holder.tvLike.setTextColor(context.getResources().getColor(
                liked ? R.color.badge_red : R.color.text_secondary));

        holder.btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onLike(moment);
                }
            }
        });
        holder.btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onComment(moment, null);
                }
            }
        });
    }

    /** 点赞行 + 每条评论一行 */
    private void bindLikeAndComment(ViewHolder holder, final MomentBean moment) {
        List<String> likes = moment.getLikes();
        List<MomentBean.CommentBean> comments = moment.getComments();
        boolean hasLike = likes != null && !likes.isEmpty();
        boolean hasComment = comments != null && !comments.isEmpty();

        if (!hasLike && !hasComment) {
            holder.llLikeComment.setVisibility(View.GONE);
            return;
        }
        holder.llLikeComment.setVisibility(View.VISIBLE);

        if (hasLike) {
            holder.tvLikes.setVisibility(View.VISIBLE);
            holder.tvLikes.setText(buildLikeText(likes));
        } else {
            holder.tvLikes.setVisibility(View.GONE);
        }
        holder.vDivider.setVisibility(hasLike && hasComment ? View.VISIBLE : View.GONE);

        holder.llComments.removeAllViews();
        if (!hasComment) {
            holder.llComments.setVisibility(View.GONE);
            return;
        }
        holder.llComments.setVisibility(View.VISIBLE);
        for (final MomentBean.CommentBean comment : comments) {
            TextView tv = new TextView(context);
            tv.setTextSize(13.5f);
            tv.setTextColor(context.getResources().getColor(R.color.text_primary));
            tv.setLineSpacing(dp(2), 1f);
            tv.setPadding(0, dp(2), 0, dp(2));
            tv.setText(buildCommentText(comment));
            tv.setBackgroundResource(android.R.drawable.list_selector_background);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        // 点别人的评论 = 回复他；点自己的评论 = 继续补充
                        listener.onComment(moment, comment);
                    }
                }
            });
            tv.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (listener != null) {
                        listener.onDeleteComment(moment, comment);
                    }
                    return true;
                }
            });
            holder.llComments.addView(tv);
        }
    }

    private CharSequence buildLikeText(List<String> likes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < likes.size(); i++) {
            if (i > 0) {
                sb.append("，");
            }
            String name = likes.get(i);
            sb.append(MomentBean.ME_TAG.equals(name) ? AppPrefs.getMyNick() : name);
        }
        sb.append(" ").append(context.getString(R.string.liked_by));
        return sb.toString();
    }

    /** 「张三：好看」或者「我 回复 张三：哈哈」 */
    private CharSequence buildCommentText(MomentBean.CommentBean comment) {
        String user = comment.isMine() ? "我" : safe(comment.getUser());
        SpannableStringBuilder sb = new SpannableStringBuilder();
        int start = sb.length();
        sb.append(user);
        sb.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.link_blue)),
                start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (!isEmpty(comment.getReplyTo())) {
            sb.append(" 回复 ");
            start = sb.length();
            sb.append(comment.getReplyTo());
            sb.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.link_blue)),
                    start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        sb.append("：").append(safe(comment.getText()));
        return sb;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    static class ViewHolder {
        CircleImageView ivAvatar;
        TextView tvName;
        TextView tvMineTag;
        TextView tvContent;
        FlowLayout llImages;
        TextView tvTime;
        LinearLayout btnComment;
        LinearLayout btnLike;
        ImageView ivLike;
        TextView tvLike;
        LinearLayout llLikeComment;
        TextView tvLikes;
        View vDivider;
        LinearLayout llComments;
    }
}
