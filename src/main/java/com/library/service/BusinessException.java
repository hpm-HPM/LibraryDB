package com.library.service;

/**
 * 业务异常。Servlet 捕获后以 {code:1,message:...} 返回给前端。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }
}