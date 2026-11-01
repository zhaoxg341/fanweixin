package cn.itcast.weixin.bean;

/**
 * 联系人（通讯录 Tab）
 * letter 为姓名首字母，用于分组和右侧字母索引栏定位
 */
public class ContactBean {

    private int id;
    private String name;
    private String letter;
    private String avatar;
    private String weixinId;
    private String region;
    /** 备注名。由 RemarkDao 从数据库填进来，展示时优先用备注 */
    private String remark;

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /** 显示用名字：有备注就显示备注，没有就显示昵称 */
    public String getDisplayName() {
        if (remark != null && !remark.trim().isEmpty()) {
            return remark;
        }
        return name;
    }

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

    public String getLetter() {
        return letter == null ? "#" : letter.toUpperCase();
    }

    public void setLetter(String letter) {
        this.letter = letter;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getWeixinId() {
        return weixinId;
    }

    public void setWeixinId(String weixinId) {
        this.weixinId = weixinId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
