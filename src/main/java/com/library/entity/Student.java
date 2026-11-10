package com.library.entity;

/**
 * 学生。
 */
public class Student extends User {

    private static final long serialVersionUID = 1L;

    private String grade;
    private String college;

    @Override
    public String[] hasPermission() {
        return new String[]{"借书", "还书", "个人借阅查询"};
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }
}