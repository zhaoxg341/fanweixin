package cn.itcast.weixin.utils;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import cn.itcast.weixin.bean.ChatBean;
import cn.itcast.weixin.bean.ContactBean;
import cn.itcast.weixin.bean.MomentBean;
import cn.itcast.weixin.bean.UserBean;

/**
 * 数据工具类：读取 assets/weixin.json 并解析成实体类。
 * 把 Constant.USE_SERVER 改为 true 即可切换成网络请求。
 */
public class WeixinDataUtils {

    /** assets 里那份「本机默认用户」，启动时预热一次，之后直接读内存 */
    private static UserBean localUser;

    /**
     * 启动时预热。JSON 只有几 KB，放在 Application.onCreate 里读一次可以接受，
     * 换来的好处是 AppPrefs 这种纯静态工具类也能同步拿到默认昵称/头像。
     */
    public static void warmUp(Context context) {
        if (localUser == null) {
            localUser = parseUser(loadWeixinJson(context));
        }
    }

    public static UserBean getLocalUser() {
        return localUser;
    }

    /** 获取 JSON 字符串 */
    public static String loadWeixinJson(Context context) {
        if (Constant.USE_SERVER) {
            String json = HttpUtil.get(Constant.WEIXIN_JSON_URL);
            if (json != null && !json.trim().isEmpty()) {
                return json;
            }
        }
        return readAssets(context, "weixin.json");
    }

    /** 解析当前用户 */
    public static UserBean parseUser(String json) {
        UserBean user = new UserBean();
        try {
            JSONObject obj = new JSONObject(json).optJSONObject("user");
            if (obj != null) {
                user.setNickName(obj.optString("nickName"));
                user.setWeixinId(obj.optString("weixinId"));
                user.setAvatar(obj.optString("avatar"));
                user.setRegion(obj.optString("region"));
                user.setSignature(obj.optString("signature"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }

    /** 解析会话列表 */
    public static List<ChatBean> parseChatList(String json) {
        List<ChatBean> list = new ArrayList<>();
        try {
            JSONArray array = new JSONObject(json).optJSONArray("chats");
            if (array == null) {
                return list;
            }
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                ChatBean chat = new ChatBean();
                chat.setId(obj.optInt("id"));
                chat.setName(obj.optString("name"));
                chat.setAvatar(obj.optString("avatar"));
                chat.setLastMsg(obj.optString("lastMsg"));
                chat.setTime(obj.optString("time"));
                chat.setUnread(obj.optInt("unread"));
                chat.setGroup(obj.optBoolean("group"));
                list.add(chat);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 解析联系人列表 */
    public static List<ContactBean> parseContactList(String json) {
        List<ContactBean> list = new ArrayList<>();
        try {
            JSONArray array = new JSONObject(json).optJSONArray("contacts");
            if (array == null) {
                return list;
            }
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                ContactBean c = new ContactBean();
                c.setId(obj.optInt("id"));
                c.setName(obj.optString("name"));
                c.setLetter(obj.optString("letter"));
                c.setAvatar(obj.optString("avatar"));
                c.setWeixinId(obj.optString("weixinId"));
                c.setRegion(obj.optString("region"));
                list.add(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 解析朋友圈动态 */
    public static List<MomentBean> parseMomentList(String json) {
        List<MomentBean> list = new ArrayList<>();
        try {
            JSONArray array = new JSONObject(json).optJSONArray("moments");
            if (array == null) {
                return list;
            }
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                MomentBean m = new MomentBean();
                m.setId(obj.optInt("id"));
                m.setName(obj.optString("name"));
                m.setAvatar(obj.optString("avatar"));
                m.setContent(obj.optString("content"));
                m.setImages(optStringList(obj.optJSONArray("images")));
                m.setTime(obj.optString("time"));
                m.setLikes(optStringList(obj.optJSONArray("likes")));
                JSONArray cs = obj.optJSONArray("comments");
                List<MomentBean.CommentBean> comments = new ArrayList<>();
                if (cs != null) {
                    for (int j = 0; j < cs.length(); j++) {
                        JSONObject co = cs.getJSONObject(j);
                        MomentBean.CommentBean cb = new MomentBean.CommentBean();
                        cb.setUser(co.optString("user"));
                        cb.setText(co.optString("text"));
                        comments.add(cb);
                    }
                }
                m.setComments(comments);
                list.add(m);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static List<String> optStringList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                list.add(array.optString(i));
            }
        }
        return list;
    }

    /**
     * 根据图片名称取得 drawable 资源 id。
     * JSON 里写的是带扩展名的文件名（如 avatar1.jpg），而资源名不能带点，
     * 所以这里先把扩展名去掉再查。
     */
    public static int getDrawableId(Context context, String name) {
        if (name == null || name.trim().isEmpty()) {
            return 0;
        }
        String key = name.trim();
        int dot = key.lastIndexOf('.');
        if (dot > 0) {
            key = key.substring(0, dot);
        }
        return context.getResources().getIdentifier(key, "drawable", context.getPackageName());
    }

    /** 读取 assets 目录下的文本文件 */
    private static String readAssets(Context context, String fileName) {
        InputStream is = null;
        try {
            is = context.getAssets().open(fileName);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }
}
