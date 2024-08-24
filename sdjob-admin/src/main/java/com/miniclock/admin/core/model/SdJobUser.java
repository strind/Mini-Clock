package com.miniclock.admin.core.model;

/**
 * @author strind
 * @date 2024/8/24 14:11
 * @description 用户消息对应的实体
 */
public class SdJobUser {
    //用户id
    private int id;
    //用户名
    private String username;
    //密码
    private String password;
    //用户角色，0是普通用户，1是管理员
    private int role;
    //对应权限
    private String permission;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
}
