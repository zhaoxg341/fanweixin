package cn.itcast.weixin.bean;

/**
 * 收藏条目：长按聊天消息「收藏」后存入 t_favorite，"我-收藏"页面展示。
 * type 与 MsgBean 一致（0 文字 1 图片 2 红包 3 语音 4 转账 5 拍一拍），
 * content 原样保存（红包/转账仍是 JSON），time 为收藏时刻。
 */
public class FavoriteBean {

    private int id;
    private int chatId;
    private String fromName;
    private boolean fromMe;
    private int type;
    private String content;
    private String time;
    private long ts;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getChatId() {
        return chatId;
    }

    public void setChatId(int chatId) {
        this.chatId = chatId;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public boolean isFromMe() {
        return fromMe;
    }

    public void setFromMe(boolean fromMe) {
        this.fromMe = fromMe;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    /** 列表里显示的预览文字 */
    public String preview() {
        switch (type) {
            case MsgBean.TYPE_IMAGE:
                return "[图片]";
            case MsgBean.TYPE_REDPACKET:
                return "[微信红包]";
            case MsgBean.TYPE_VOICE:
                return "[语音]";
            case MsgBean.TYPE_TRANSFER:
                return "[转账]";
            default:
                return content == null ? "" : content;
        }
    }
}
