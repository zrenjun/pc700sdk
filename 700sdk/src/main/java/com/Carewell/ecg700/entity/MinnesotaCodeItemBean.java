package com.Carewell.ecg700.entity;

/**
 * Created by wxd on 2019-04-29.
 */

public class MinnesotaCodeItemBean {

    private int id;//数据库表使用
    private String code;
    private String title;
    private String level;
    private String minnesotaCode;
    private String lead;
    private String userId;
    private long createTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMinnesotaCode() {
        return minnesotaCode;
    }

    public void setMinnesotaCode(String minnesotaCode) {
        this.minnesotaCode = minnesotaCode;
    }

    public String getLead() {
        return lead;
    }

    public void setLead(String lead) {
        this.lead = lead;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
}
