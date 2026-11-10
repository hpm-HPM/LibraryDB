package com.library.servlet;

import com.library.entity.User;
import com.library.service.BusinessException;
import com.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 所有接口 Servlet 的基类：统一异常处理和参数读取。
 */
public abstract class BaseServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        try {
            String method = req.getMethod();
            if ("GET".equalsIgnoreCase(method)) {
                handleGet(req, resp);
            } else if ("POST".equalsIgnoreCase(method)) {
                handlePost(req, resp);
            } else {
                JsonUtil.write(resp, 405, 1, "不支持的请求方法", null);
            }
        } catch (AuthException e) {
            JsonUtil.write(resp, e.getStatus(), 1, e.getMessage(), null);
        } catch (BusinessException e) {
            JsonUtil.fail(resp, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            if (!resp.isCommitted()) {
                JsonUtil.write(resp, 500, 1, "服务器处理失败，请稍后重试", null);
            }
        }
    }

    protected void handleGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonUtil.write(resp, 405, 1, "不支持的请求方法", null);
    }

    protected void handlePost(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonUtil.write(resp, 405, 1, "不支持的请求方法", null);
    }

    protected Map<String, String> params(HttpServletRequest req) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
            String[] values = entry.getValue();
            if (values != null && values.length > 0) {
                result.put(entry.getKey(), values[0]);
            }
        }
        return result;
    }

    protected String param(HttpServletRequest req, String name) {
        return req.getParameter(name);
    }

    protected User currentUser(HttpServletRequest req) {
        Object value = req.getSession(false) == null ? null
                : req.getSession(false).getAttribute("loginUser");
        return value instanceof User ? (User) value : null;
    }

    protected User requireLogin(HttpServletRequest req) {
        User user = currentUser(req);
        if (user == null) {
            throw new AuthException(401, "请先登录");
        }
        return user;
    }

    /**
     * 按角色功能权限校验。User.hasPermission() 与前端功能卡使用同一套中文名称。
     */
    protected User requirePermission(HttpServletRequest req, String permission) {
        User user = requireLogin(req);
        String[] permissions = user.hasPermission();
        if (permissions != null && Arrays.asList(permissions).contains(permission)) {
            return user;
        }
        throw new AuthException(403, "当前角色没有【" + permission + "】权限");
    }

    protected void notFound() {
        throw new BusinessException("接口不存在");
    }
}