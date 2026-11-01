package cn.itcast.order.bean;

/**
 * 店铺评价
 */
public class ReviewBean {

    private String userName;
    private float score;
    private String content;
    private String time;
    private String tag;

    public ReviewBean() {
    }

    public ReviewBean(String userName, float score, String content, String time, String tag) {
        this.userName = userName;
        this.score = score;
        this.content = content;
        this.time = time;
        this.tag = tag;
    }

    public String getUserName() {
        return userName == null ? "" : userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public String getContent() {
        return content == null ? "" : content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTime() {
        return time == null ? "" : time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTag() {
        return tag == null ? "" : tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    /** 昵称首字，用作头像文字 */
    public String getAvatarText() {
        return getUserName().isEmpty() ? "美" : getUserName().substring(0, 1);
    }
}
