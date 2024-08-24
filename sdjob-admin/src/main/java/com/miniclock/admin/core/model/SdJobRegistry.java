package com.miniclock.admin.core.model;

import java.util.Date;
import java.util.List;

/**
 * @author strind
 * @date 2024/8/23 16:03
 * @description 执行器的实体类
 */
public class SdJobRegistry {

    // 执行器的id
    private Integer id;
    // 注册类型 自动/手动
    private String registryGroup;
    // 执行器的appName
    private String registryKey;
    // 执行器的地址
    private String registryValue;
    private Date updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRegistryGroup() {
        return registryGroup;
    }

    public void setRegistryGroup(String registryGroup) {
        this.registryGroup = registryGroup;
    }

    public String getRegistryKey() {
        return registryKey;
    }

    public void setRegistryKey(String registryKey) {
        this.registryKey = registryKey;
    }

    public String getRegistryValue() {
        return registryValue;
    }

    public void setRegistryValue(String registryValue) {
        this.registryValue = registryValue;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
