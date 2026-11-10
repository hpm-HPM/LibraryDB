package com.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注实体类对应的数据库表名。默认使用类名小写。
 * User 对应 SQL Server 的保留字表 [user]，因此用该注解显式指定。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface TableName {
    String value();
}