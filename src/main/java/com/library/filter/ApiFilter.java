package com.library.filter;

import com.util.JsonUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 统一 UTF-8 编码并校验 API 登录状态。
 */
@WebFilter("/api/*")
public class ApiFilter implements Filter {

    private static final Set<String> WHITELIST = new HashSet<>(Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/me",
            "/api/captcha"
    ));

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (WHITELIST.contains(path)) {
            chain.doFilter(request, response);
            return;
        }
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("loginUser") == null) {
            JsonUtil.write(resp, 401, 1, "请先登录", null);
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}