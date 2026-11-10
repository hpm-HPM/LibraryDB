package com.library.servlet;

import com.library.entity.User;
import com.library.service.UserService;
import com.util.Dto;
import com.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 管理员用户信息查询。
 * 约定：请求不携带 keyword 参数时返回空列表（默认不显示数据）；
 * 携带 keyword（可以为空字符串）时执行模糊查询。
 */
@WebServlet("/api/users/*")
public class UserServlet extends BaseServlet {

    private final UserService userService = new UserService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String action = req.getPathInfo();
        if (action == null || "/".equals(action) || "/search".equals(action)) {
            search(req, resp);
        } else if ("/detail".equals(action)) {
            detail(req, resp);
        } else {
            notFound();
        }
    }

    private void search(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        requirePermission(req, "用户信息查询");
        if (!req.getParameterMap().containsKey("keyword")) {
            JsonUtil.ok(resp, Collections.emptyList());
            return;
        }
        List<User> users = userService.search(req.getParameter("keyword"));
        List<Map<String, Object>> data = new ArrayList<>();
        for (User user : users) {
            data.add(Dto.user(user));
        }
        JsonUtil.ok(resp, data);
    }

    private void detail(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        requirePermission(req, "用户信息查询");
        String id = req.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            throw new com.library.service.BusinessException("用户ID不能为空");
        }
        User user = userService.detail(id.trim());
        if (user == null) {
            throw new com.library.service.BusinessException("用户不存在");
        }
        JsonUtil.ok(resp, Dto.user(user));
    }
}