package com.library.servlet;

/**
 * 需要以指定 HTTP 状态码返回的认证/授权异常。
 */
public class AuthException extends RuntimeException {

    private final int status;

    public AuthException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}