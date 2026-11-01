package cn.itcast.weixin.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.itcast.weixin.bean.ChatBean;
import cn.itcast.weixin.bean.MsgBean;
import cn.itcast.weixin.db.ExtraChatDao;
import cn.itcast.weixin.db.MessageDao;

/**
 * 会话与消息的内存数据中心（单例风格）。
 *
 * 相比第一版，多了三件事：
 * 1. 消息会写进 SQLite（MessageDao），退出 App 再进来聊天记录还在；
 * 2. 加了监听器机制，在线模式下别人发来消息时，会话列表和聊天页能自动刷新；
 * 3. send() 在局域网模式下会把消息真的发出去（ChatClient → ChatServer → 对方手机）。
 *
 * 注意：所有回调都保证在主线程触发，调用方不用再切线程。
 */
public class ChatManager {

    /** 会话有新消息 / 数据变化时回调，chatId 是变化的那个会话 */
    public interface OnChatChangedListener {
        void onChatChanged(int chatId);
    }

    private static final List<ChatBean> CHATS = new ArrayList<>();
    private static final Map<Integer, List<MsgBean>> MSG_MAP = new HashMap<>();
    private static final List<OnChatChangedListener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm", Locale.CHINA);

    /** 在线聊天新建的会话从这里开始分配 id，避开 assets 里已有的 1~8 */
    private static final int EXTRA_ID_START = 1000;

    private static int nextChatId = EXTRA_ID_START;
    private static boolean inited = false;

    private ChatManager() {
    }

    /** 基础会话列表是否已经加载过（决定要不要去读 assets） */
    public static boolean isInited() {
        return inited;
    }

    /** 应用启动后由微信 Tab 首次加载时调用 */
    public static synchronized void init(List<ChatBean> chats) {
        if (inited) {
            return;
        }
        if (chats != null) {
            CHATS.addAll(chats);
        }
        // 把上一次在线聊天新建、存在数据库里的会话合并进来
        for (ChatBean extra : ExtraChatDao.queryAll()) {
            if (findChat(extra.getId()) == null && findChatByName(extra.getName()) == null) {
                CHATS.add(extra);
            }
        }
        for (ChatBean chat : CHATS) {
            if (nextChatId <= chat.getId()) {
                nextChatId = chat.getId() + 1;
            }
            MSG_MAP.put(chat.getId(), loadMessages(chat));
        }
        inited = true;
    }

    /**
     * 取某个会话的历史消息：优先数据库。
     * 数据库里没有（首次安装）就按会话的最后一条消息造一条并落库，
     * 这样聊天页打开就有内容，而不是空白。
     */
    private static List<MsgBean> loadMessages(ChatBean chat) {
        List<MsgBean> list = new ArrayList<>(MessageDao.query(chat.getId()));
        if (!list.isEmpty()) {
            return list;
        }
        String lastMsg = safe(chat.getLastMsg());
        String time = safe(chat.getTime());
        MsgBean first = chat.isGroup()
                ? new MsgBean("班长：" + lastMsg, false, time)
                : new MsgBean(lastMsg, false, time);
        MessageDao.insert(chat.getId(), first);
        list.add(first);
        return list;
    }

    public static List<ChatBean> getChats() {
        return CHATS;
    }

    public static ChatBean findChat(int chatId) {
        for (ChatBean chat : CHATS) {
            if (chat.getId() == chatId) {
                return chat;
            }
        }
        return null;
    }

    public static ChatBean findChatByName(String name) {
        if (name == null) {
            return null;
        }
        for (ChatBean chat : CHATS) {
            if (name.equals(chat.getName())) {
                return chat;
            }
        }
        return null;
    }

    public static List<MsgBean> getMessages(int chatId) {
        List<MsgBean> list = MSG_MAP.get(chatId);
        if (list == null) {
            list = new ArrayList<>();
            MSG_MAP.put(chatId, list);
        }
        return list;
    }

    /** 清空某个会话的聊天记录（内存和数据库一起清），头像、备注不受影响 */
    public static void clearMessages(int chatId) {
        MessageDao.deleteByChat(chatId);
        List<MsgBean> list = MSG_MAP.get(chatId);
        if (list != null) {
            list.clear();
        }
        ChatBean chat = findChat(chatId);
        if (chat != null) {
            chat.setLastMsg("");
            chat.setTime("");
            chat.setUnread(0);
        }
        notifyChanged(chatId);
    }

    /** 进入聊天页时调用：清掉未读红点 */
    public static void clearUnread(int chatId) {
        ChatBean chat = findChat(chatId);
        if (chat != null && chat.getUnread() != 0) {
            chat.setUnread(0);
            notifyChanged(chatId);
        }
    }

    /**
     * 发送一条消息：追加到消息列表、落库、更新会话的最后一条消息和时间。
     * 局域网模式下会真正通过 Socket 发给对方。
     */
    public static void send(int chatId, String content) {
        ChatBean chat = findChat(chatId);
        if (chat == null || content == null || content.trim().isEmpty()) {
            return;
        }
        String text = content.trim();
        String time = TIME_FMT.format(new Date());
        MsgBean msg = new MsgBean(text, true, time);
        getMessages(chatId).add(msg);
        MessageDao.insert(chatId, msg);
        chat.setLastMsg(text);
        chat.setTime(time);
        if (AppPrefs.isLanMode()) {
            ChatClient.get().send(chat.getName(), text);
        }
        notifyChanged(chatId);
    }

    /**
     * 发送一张图片：content 存本机图片路径，聊天页按 type 渲染成缩略图。
     *
     * 图片本身没有做「上传」，所以局域网模式下只告诉对方「[图片]」，
     * 不至于对方那边收到一条打不开的本地路径。
     */
    public static void sendImage(int chatId, String path) {
        ChatBean chat = findChat(chatId);
        if (chat == null || path == null || path.trim().isEmpty()) {
            return;
        }
        String time = TIME_FMT.format(new Date());
        MsgBean msg = new MsgBean(path.trim(), true, time, MsgBean.TYPE_IMAGE);
        getMessages(chatId).add(msg);
        MessageDao.insert(chatId, msg);
        chat.setLastMsg(msg.summary());
        chat.setTime(time);
        if (AppPrefs.isLanMode()) {
            ChatClient.get().send(chat.getName(), msg.summary());
        }
        notifyChanged(chatId);
    }

    /** 收到一条消息（本地演示模式下的模拟自动回复） */
    public static void receive(int chatId, String content) {
        ChatBean chat = findChat(chatId);
        if (chat == null) {
            return;
        }
        String time = TIME_FMT.format(new Date());
        MsgBean msg = new MsgBean(content, false, time);
        getMessages(chatId).add(msg);
        MessageDao.insert(chatId, msg);
        chat.setLastMsg(content);
        chat.setTime(time);
        notifyChanged(chatId);
    }

    // ---------------- 红包 / 语音 / 撤回 ----------------

    /** 发一个红包：content 存 JSON（留言 / 金额 / 是否已领取） */
    public static MsgBean sendRedPacket(int chatId, String greeting, String amount) {
        ChatBean chat = findChat(chatId);
        if (chat == null) {
            return null;
        }
        String time = TIME_FMT.format(new Date());
        MsgBean msg = new MsgBean(MsgBean.buildRedPacketContent(greeting, amount),
                true, time, MsgBean.TYPE_REDPACKET);
        getMessages(chatId).add(msg);
        MessageDao.insert(chatId, msg);
        chat.setLastMsg(msg.summary());
        chat.setTime(time);
        if (AppPrefs.isLanMode()) {
            ChatClient.get().send(chat.getName(), msg.summary());
        }
        notifyChanged(chatId);
        return msg;
    }

    /** 对方发来一个红包（演示：聊到「红包」关键词时触发） */
    public static MsgBean receiveRedPacket(int chatId, String greeting, String amount) {
        ChatBean chat = findChat(chatId);
        if (chat == null) {
            return null;
        }
        String time = TIME_FMT.format(new Date());
        MsgBean msg = new MsgBean(MsgBean.buildRedPacketContent(greeting, amount),
                false, time, MsgBean.TYPE_REDPACKET);
        getMessages(chatId).add(msg);
        MessageDao.insert(chatId, msg);
        chat.setLastMsg(msg.summary());
        chat.setTime(time);
        notifyChanged(chatId);
        return msg;
    }

    /** 点开红包：把 JSON 里的 opened 置为 true 并落库 */
    public static void openRedPacket(int chatId, MsgBean msg) {
        if (msg == null || msg.redPacketOpened()) {
            return;
        }
        msg.setContent(msg.markRedPacketOpened());
        MessageDao.updateContent(msg.getId(), msg.getContent());
        notifyChanged(chatId);
    }

    /** 发一条语音：content 存秒数 */
    public static void sendVoice(int chatId, int seconds) {
        ChatBean chat = findChat(chatId);
        if (chat == null) {
            return;
        }
        int dur = Math.max(1, Math.min(60, seconds));
        String time = TIME_FMT.format(new Date());
        MsgBean msg = new MsgBean(String.valueOf(dur), true, time, MsgBean.TYPE_VOICE);
        getMessages(chatId).add(msg);
        MessageDao.insert(chatId, msg);
        chat.setLastMsg(msg.summary());
        chat.setTime(time);
        notifyChanged(chatId);
    }

    /** 对方发来一条语音（演示用：偶尔用语音回复） */
    public static void receiveVoice(int chatId, int seconds) {
        ChatBean chat = findChat(chatId);
        if (chat == null) {
            return;
        }
        String time = TIME_FMT.format(new Date());
        MsgBean msg = new MsgBean(String.valueOf(Math.max(1, seconds)), false, time,
                MsgBean.TYPE_VOICE);
        getMessages(chatId).add(msg);
        MessageDao.insert(chatId, msg);
        chat.setLastMsg(msg.summary());
        chat.setTime(time);
        notifyChanged(chatId);
    }

    // ---------------- 转账 / 拍一拍 ----------------

    /** 发起一笔转账：content 存 JSON（留言 / 金额 / 收款状态） */
    public static MsgBean sendTransfer(int chatId, String note, String amount) {
        ChatBean chat = findChat(chatId);
        if (chat == null) {
            return null;
        }
        String time = TIME_FMT.format(new Date());
        MsgBean msg = new MsgBean(MsgBean.buildTransferContent(note, amount),
                true, time, MsgBean.TYPE_TRANSFER);
        getMessages(chatId).add(msg);
        MessageDao.insert(chatId, msg);
        chat.setLastMsg(msg.summary());
        chat.setTime(time);
        if (AppPrefs.isLanMode()) {
            ChatClient.get().send(chat.getName(), msg.summary());
        }
        notifyChanged(chatId);
        return msg;
    }

    /** 对方转来一笔钱（演示模式用） */
    public static MsgBean receiveTransfer(int chatId, String note, String amount) {
        ChatBean chat = findChat(chatId);
        if (chat == null) {
            return null;
        }
        String time = TIME_FMT.format(new Date());
        MsgBean msg = new MsgBean(MsgBean.buildTransferContent(note, amount),
                false, time, MsgBean.TYPE_TRANSFER);
        getMessages(chatId).add(msg);
        MessageDao.insert(chatId, msg);
        chat.setLastMsg(msg.summary());
        chat.setTime(time);
        notifyChanged(chatId);
        return msg;
    }

    /** 收款：把 JSON 里的 state 置为 1 并落库 */
    public static void acceptTransfer(int chatId, MsgBean msg) {
        if (msg == null || msg.transferReceived()) {
            return;
        }
        msg.setContent(msg.markTransferReceived());
        MessageDao.updateContent(msg.getId(), msg.getContent());
        notifyChanged(chatId);
    }

    /**
     * 拍一拍：一条居中的灰色系统提示（不占气泡）。
     * fromMe=true 是"你拍了拍…"，false 是对方拍你。
     */
    public static void sendPat(int chatId, String text) {
        ChatBean chat = findChat(chatId);
        if (chat == null || text == null || text.trim().isEmpty()) {
            return;
        }
        String time = TIME_FMT.format(new Date());
        MsgBean msg = new MsgBean(text.trim(), true, time, MsgBean.TYPE_PAT);
        getMessages(chatId).add(msg);
        MessageDao.insert(chatId, msg);
        chat.setLastMsg(msg.summary());
        chat.setTime(time);
        notifyChanged(chatId);
    }

    /** 对方拍了我一下 */
    public static void receivePat(int chatId, String text) {
        ChatBean chat = findChat(chatId);
        if (chat == null || text == null || text.trim().isEmpty()) {
            return;
        }
        String time = TIME_FMT.format(new Date());
        MsgBean msg = new MsgBean(text.trim(), false, time, MsgBean.TYPE_PAT);
        getMessages(chatId).add(msg);
        MessageDao.insert(chatId, msg);
        chat.setLastMsg(msg.summary());
        chat.setTime(time);
        notifyChanged(chatId);
    }

    /** 撤回一条消息：状态置 1（2 分钟内、自己发的才允许，判断在 MsgBean.canRecall） */
    public static void recall(int chatId, MsgBean msg) {
        if (msg == null || msg.isRecalled()) {
            return;
        }
        msg.setStatus(1);
        MessageDao.markRecalled(msg.getId());
        refreshLastMsg(chatId);
        notifyChanged(chatId);
    }

    /** 删除一条消息（只删自己这边的记录，撤回提示不受影响） */
    public static void deleteMsg(int chatId, MsgBean msg) {
        if (msg == null) {
            return;
        }
        getMessages(chatId).remove(msg);
        MessageDao.deleteById(msg.getId());
        refreshLastMsg(chatId);
        notifyChanged(chatId);
    }

    /** 会话列表的最后一条消息按「最后一条未撤回的消息」重新算 */
    private static void refreshLastMsg(int chatId) {
        ChatBean chat = findChat(chatId);
        if (chat == null) {
            return;
        }
        List<MsgBean> list = getMessages(chatId);
        for (int i = list.size() - 1; i >= 0; i--) {
            MsgBean m = list.get(i);
            if (!m.isRecalled()) {
                chat.setLastMsg(m.summary());
                chat.setTime(m.getTime());
                return;
            }
        }
        chat.setLastMsg("");
    }

    /**
     * 收到对方设备发来的真实消息（局域网模式）。
     * 对方可能不在会话列表里，这时会自动建一个新会话。
     */
    public static void receiveRemote(String fromName, String content) {
        if (fromName == null || fromName.trim().isEmpty()) {
            return;
        }
        ChatBean chat = findOrCreateChat(fromName.trim());
        String time = TIME_FMT.format(new Date());
        MsgBean msg = new MsgBean(content, false, time);
        getMessages(chat.getId()).add(msg);
        MessageDao.insert(chat.getId(), msg);
        chat.setLastMsg(content);
        chat.setTime(time);
        chat.setUnread(chat.getUnread() + 1);
        ExtraChatDao.upsert(chat);
        notifyChanged(chat.getId());
    }

    /** 按昵称找会话，找不到就新建一个（并落库，重启后还在） */
    public static synchronized ChatBean findOrCreateChat(String name) {
        ChatBean chat = findChatByName(name);
        if (chat != null) {
            return chat;
        }
        chat = new ChatBean();
        chat.setId(nextChatId++);
        chat.setName(name);
        chat.setAvatar("avatar_default");
        chat.setLastMsg("");
        chat.setTime("");
        chat.setUnread(0);
        chat.setGroup(false);
        CHATS.add(chat);
        MSG_MAP.put(chat.getId(), new ArrayList<MsgBean>());
        ExtraChatDao.upsert(chat);
        return chat;
    }

    /** 全部未读数（微信 Tab 底部红点用） */
    public static int totalUnread() {
        int total = 0;
        for (ChatBean chat : CHATS) {
            total += chat.getUnread();
        }
        return total;
    }

    // ---------------- 监听器 ----------------

    public static void addListener(OnChatChangedListener listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(OnChatChangedListener listener) {
        LISTENERS.remove(listener);
    }

    /** 广播数据变化（必须在主线程调用）。改备注 / 改头像后也用它通知界面重画 */
    public static void notifyChanged(int chatId) {
        for (OnChatChangedListener listener : LISTENERS) {
            listener.onChatChanged(chatId);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
