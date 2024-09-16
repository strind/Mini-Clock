package com.miniclock.core.biz.model;

import java.io.Serializable;

/**
 * @author strind
 * @date 2024/8/23 16:30
 * @description 执行器注册信息
 */
public class RegistryParam implements Serializable {

    public static final long SerialVersionUID = -1L;

    // 注册方式
    private String registryGroup;

    // 执行器的唯一标识
    private String registryKey;
    // 执行器地址
    private String registryValue;

    public RegistryParam(String registryGroup, String registryKey, String getRegistryValue) {
        this.registryGroup = registryGroup;
        this.registryKey = registryKey;
        this.registryValue = getRegistryValue;
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

    @Override
    public String toString() {
        return "RegistryParam{" +
            "registryGroup='" + registryGroup + '\'' +
            ", registryKey='" + registryKey + '\'' +
            ", getRegistryValue='" + registryValue + '\'' +
            '}';
    }
}
