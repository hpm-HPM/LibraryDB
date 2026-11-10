package com.library.entity;

import com.util.TableName;

import java.time.LocalDateTime;

/**
 * 用户抽象基类，角色由子类 Admin / Operator / Student / Teacher 体现。
 * user 是 SQL Server 保留字，因此显式指定表名。
 */
@TableName("user")
public abstract class User extends Base {

    private static final long serialVersionUID = 1L;

    private String name;
    private String password;
    private String phone;
    private String role;
    private String major;
    private LocalDateTime loginTime;

    /**
     * 当前角色拥有的功能权限名称，前端据此显示功能卡。
     */
    public abstract String[] hasPermission();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }
}