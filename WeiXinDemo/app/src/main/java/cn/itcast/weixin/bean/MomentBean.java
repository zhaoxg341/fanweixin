package cn.itcast.weixin.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * 朋友圈动态
 *
 * 除了从 assets/weixin.json 读出来的字段，还多了三个「本机产生」的字段：
 * ts    —— 发布时间戳，列表按它倒序（后发的在最上面）
 * mine  —— 是不是我自己发的，决定能不能删除
 * 评论里的 mine / replyTo 同理：mine 判断删除权限，replyTo 记录「回复谁」
 */
public class MomentBean {

    private int id;
    private String name;
    private String avatar;
    private String content;
    private List<String> images = new ArrayList<>();
    private String time;
    private long ts;
    private boolean mine;
    private List<String> likes = new ArrayList<>();
    private List<CommentBean> comments = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images == null ? new ArrayList<String>() : images;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public boolean isMine() {
        return mine;
    }

    public void setMine(boolean mine) {
        this.mine = mine;
    }

    public List<String> getLikes() {
        return likes;
    }

    public void setLikes(List<String> likes) {
        this.likes = likes == null ? new ArrayList<String>() : likes;
    }

    public List<CommentBean> getComments() {
        return comments;
    }

    public void setComments(List<CommentBean> comments) {
        this.comments = comments == null ? new ArrayList<CommentBean>() : comments;
    }

    /** 我有没有给这条点过赞 */
    public boolean isLikedByMe() {
        return likes != null && likes.contains(ME_TAG);
    }

    /** 点赞列表里用这个占位符表示「我」，显示时再换成当前昵称，改了昵称也不会错 */
    public static final String ME_TAG = "@me";

    /** 一条评论 */
    public static class CommentBean {
        private int id;
        private String user;
        private String text;
        /** 回复谁的；为 null 表示直接评论这条动态 */
        private String replyTo;
        /** 是不是我写的（只有我的评论能长按删除） */
        private boolean mine;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getReplyTo() {
            return replyTo;
        }

        public void setReplyTo(String replyTo) {
            this.replyTo = replyTo;
        }

        public boolean isMine() {
            return mine;
        }

        public void setMine(boolean mine) {
            this.mine = mine;
        }
    }
}
