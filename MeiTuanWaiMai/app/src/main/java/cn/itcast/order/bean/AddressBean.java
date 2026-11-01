package cn.itcast.order.bean;

import java.io.Serializable;

/**
 * 收货地址实体（保存在 SharedPreferences 的 JSON 里）
 */
public class AddressBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private String name;      // 联系人
    private String phone;     // 手机号
    private String detail;    // 详细地址
    private boolean isDefault;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone == null ? "" : phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDetail() {
        return detail == null ? "" : detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    /** 结算页展示用的一行地址 */
    public String getFullText() {
        return getDetail();
    }
}
