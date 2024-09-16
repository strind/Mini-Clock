package com.miniclock.admin.core.model;

import java.util.*;

/**
 * @author strind
 * @date 2024/8/24 9:25
 * @description 封装 执行相同定时任务的执行器, 对应sd-job-group表
 */
public class SdJobGroup {

    private int id;
    // 执行器中的项目名称
    private String appName;
    // 中文名称
    private String title;
    // 执行器地址的注册方式 0 - 自动，1 - 手动
    private int addressRegistryType;
    // 执行器地址，IP+PORT, 不同地址之间由,分开
    private String addressList;
    private Date updateTime;
    // addressList 转换成集合
    private List<String> registryAddressList;
    public List<String> getRegistryAddressList(){
        if (addressList != null && !addressList.trim().isEmpty()){
            registryAddressList = new ArrayList<>(Arrays.asList(addressList.split(",")));
        }
        return registryAddressList;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getAddressRegistryType() {
        return addressRegistryType;
    }

    public void setAddressRegistryType(int addressRegistryType) {
        this.addressRegistryType = addressRegistryType;
    }

    public String getAddressList() {
        return addressList;
    }

    public void setAddressList(String addressList) {
        this.addressList = addressList;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public void setRegistryAddressList(List<String> registryAddressList) {
        this.registryAddressList = registryAddressList;
    }

}
