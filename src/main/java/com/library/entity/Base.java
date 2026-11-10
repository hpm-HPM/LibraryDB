package com.library.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类，对应数据库各表的 id / createTime / updateTime。
 */
public class Base implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}