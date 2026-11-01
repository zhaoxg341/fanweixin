package cn.itcast.weixin.bean;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 聊天消息
 * fromMe 为 true 表示右侧绿色气泡（我发出的），false 表示左侧白色气泡（对方发来的）
 *
 * type 区分内容形态：
 * 0 文字（content 存正文）
 * 1 图片（content 存本机图片路径）
 * 2 红包（content 存 JSON：留言、金额、有没有被领）
 * 3 语音（content 存秒数）
 * 4 转账（content 存 JSON：留言、金额、收款状态）
 * 5 拍一拍（系统提示，居中灰字，content 存提示文字）
 *
 * status：0 正常，1 已撤回（撤回后界面显示一行灰色提示）。
 * extra：预留的附加信息（暂未用，红包数据都放在 content 的 JSON 里）。
 */
public class MsgBean {

    public static final int TYPE_TEXT = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_REDPACKET = 2;
    public static final int TYPE_VOICE = 3;
    public static final int TYPE_TRANSFER = 4;
    public static final int TYPE_PAT = 5;

    /** 撤回提示的有效期：发出去 2 分钟内可以撤回，跟真微信一致 */
    public static final long RECALL_WINDOW_MS = 2 * 60 * 1000L;

    private int id;
    private String content;
    private boolean fromMe;
    private String time;
    /** 消息时间戳（毫秒）。text 形式的 time 只用于显示，排序和落库用这个 */
    private long ts;
    /** 消息类型：0 文字 1 图片 2 红包 3 语音 */
    private int type = TYPE_TEXT;
    /** 0 正常 1 已撤回 */
    private int status = 0;
    private String extra;

    public MsgBean() {
    }

    public MsgBean(String content, boolean fromMe, String time) {
        this(content, fromMe, time, TYPE_TEXT);
    }

    public MsgBean(String content, boolean fromMe, String time, int type) {
        this.content = content;
        this.fromMe = fromMe;
        this.time = time;
        this.type = type;
        this.ts = System.currentTimeMillis();
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public boolean isRecalled() {
        return status == 1;
    }

    public boolean isImage() {
        return type == TYPE_IMAGE;
    }

    public boolean isRedPacket() {
        return type == TYPE_REDPACKET;
    }

    public boolean isVoice() {
        return type == TYPE_VOICE;
    }

    public boolean isTransfer() {
        return type == TYPE_TRANSFER;
    }

    /** 拍一拍：居中灰色系统提示，不占气泡 */
    public boolean isPat() {
        return type == TYPE_PAT;
    }

    /** 这条消息现在还能不能撤回（只有自己发的、没撤过的、2 分钟内） */
    public boolean canRecall() {
        return fromMe && !isRecalled()
                && System.currentTimeMillis() - ts <= RECALL_WINDOW_MS;
    }

    // ---------------- 红包消息的 JSON 读写 ----------------

    private static final String RP_GREETING = "greeting";
    private static final String RP_AMOUNT = "amount";
    private static final String RP_OPENED = "opened";

    /** 生成红包消息的 content */
    public static String buildRedPacketContent(String greeting, String amount) {
        JSONObject json = new JSONObject();
        try {
            json.put(RP_GREETING, greeting == null || greeting.trim().isEmpty()
                    ? "恭喜发财，大吉大利" : greeting.trim());
            json.put(RP_AMOUNT, amount);
            json.put(RP_OPENED, false);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    /** 从红包消息的 content 里取字段；解析失败给一个兜底值，避免界面崩掉 */
    public String redPacketGreeting() {
        return optString(RP_GREETING, "恭喜发财，大吉大利");
    }

    public String redPacketAmount() {
        return optString(RP_AMOUNT, "0.00");
    }

    public boolean redPacketOpened() {
        JSONObject json = parse();
        return json != null && json.optBoolean(RP_OPENED, false);
    }

    /** 把红包标记成已领取，返回更新后的 JSON */
    public String markRedPacketOpened() {
        JSONObject json = parse();
        if (json == null) {
            json = new JSONObject();
        }
        try {
            json.put(RP_OPENED, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    private JSONObject parse() {
        try {
            return new JSONObject(content == null ? "{}" : content);
        } catch (JSONException e) {
            return null;
        }
    }

    // ---------------- 转账消息的 JSON 读写 ----------------

    private static final String TF_NOTE = "note";
    private static final String TF_AMOUNT = "amount";
    /** 0 待收款 1 已收款 */
    private static final String TF_STATE = "state";

    /** 生成转账消息的 content */
    public static String buildTransferContent(String note, String amount) {
        JSONObject json = new JSONObject();
        try {
            json.put(TF_NOTE, note == null ? "" : note.trim());
            json.put(TF_AMOUNT, amount);
            json.put(TF_STATE, 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    public String transferNote() {
        return optString(TF_NOTE, "转账给你");
    }

    public String transferAmount() {
        return optString(TF_AMOUNT, "0.00");
    }

    /** 0 待收款 1 已收款 */
    public int transferState() {
        JSONObject json = parse();
        return json == null ? 0 : json.optInt(TF_STATE, 0);
    }

    public boolean transferReceived() {
        return transferState() == 1;
    }

    /** 把转账标记成已收款，返回更新后的 JSON */
    public String markTransferReceived() {
        JSONObject json = parse();
        if (json == null) {
            json = new JSONObject();
        }
        try {
            json.put(TF_STATE, 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    private String optString(String key, String fallback) {
        JSONObject json = parse();
        return json == null ? fallback : json.optString(key, fallback);
    }

    /** 语音消息的秒数（content 直接存数字字符串） */
    public int voiceSeconds() {
        try {
            return Math.max(1, Integer.parseInt(content == null ? "1" : content.trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** 会话列表「最后一条消息」里显示的文字 */
    public String summary() {
        if (isRecalled()) {
            return fromMe ? "[你撤回了一条消息]" : "[对方撤回了一条消息]";
        }
        if (isImage()) {
            return "[图片]";
        }
        if (isRedPacket()) {
            return "[微信红包]";
        }
        if (isTransfer()) {
            return "[转账]";
        }
        if (isVoice()) {
            return "[语音]";
        }
        if (isPat()) {
            return content == null ? "" : content;
        }
        return content == null ? "" : content;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isFromMe() {
        return fromMe;
    }

    public void setFromMe(boolean fromMe) {
        this.fromMe = fromMe;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
