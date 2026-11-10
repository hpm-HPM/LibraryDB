package com.library.service;

import com.library.entity.Base;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 服务层通用小工具。
 */
final class ServiceSupport {

    private ServiceSupport() {
    }

    static void initBase(Base base) {
        LocalDateTime now = LocalDateTime.now();
        if (base.getId() == null) {
            base.setId(UUID.randomUUID().toString());
        }
        if (base.getCreateTime() == null) {
            base.setCreateTime(now);
        }
        base.setUpdateTime(now);
    }

    static String text(String value) {
        return value == null ? null : value.trim();
    }

    static String requireText(String value, String label) {
        String text = text(value);
        if (text == null || text.isEmpty()) {
            throw new BusinessException(label + "不能为空");
        }
        return text;
    }

    /**
     * 校验文本长度，避免超过数据库列宽后产生 500 错误。
     */
    static String maxLength(String value, int max, String label) {
        String text = text(value);
        if (text != null && text.length() > max) {
            throw new BusinessException(label + "长度不能超过 " + max + " 个字符");
        }
        return text;
    }

    static int parseInt(String value, String label, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(label + "必须是整数");
        }
    }

    static double parseDouble(String value, String label, double defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(label + "必须是数字");
        }
    }
}