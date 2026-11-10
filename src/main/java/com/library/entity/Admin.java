package com.library.entity;

import java.time.LocalDate;

/**
 * 管理员。
 */
public class Admin extends User {

    private static final long serialVersionUID = 1L;

    private Integer permissionLevel;
    private LocalDate entryTime;

    @Override
    public String[] hasPermission() {
        return new String[]{"用户信息查询", "借阅记录查询", "借阅数据统计", "借书", "还书", "审阅推荐图书"};
    }

    public Integer getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(Integer permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public LocalDate getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDate entryTime) {
        this.entryTime = entryTime;
    }
}