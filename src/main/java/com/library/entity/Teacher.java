package com.library.entity;

/**
 * 教师。
 */
public class Teacher extends User {

    private static final long serialVersionUID = 1L;

    private String college;
    private String title;

    @Override
    public String[] hasPermission() {
        return new String[]{"借书", "还书", "个人借阅查询", "推荐图书"};
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}