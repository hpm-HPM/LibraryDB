package com.library.servlet;

import com.library.entity.User;
import com.library.service.AuthService;
import com.library.service.BusinessException;
import com.util.Dto;
import com.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

/**
 * 登录、注册、退出、当前用户信息。
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String action = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/login".equals(action)) {
            login(req, resp);
        } else if ("/register".equals(action)) {
            register(req, resp);
        } else if ("/logout".equals(action)) {
            logout(req, resp);
        } else {
            notFound();
        }
    }

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String action = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/me".equals(action)) {
            User user = currentUser(req);
            JsonUtil.ok(resp, user == null ? null : Dto.user(user));
        } else {
            notFound();
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Map<String, String> form = params(req);
        HttpSession oldSession = req.getSession();
        String expected = (String) oldSession.getAttribute("captcha");
        User user = authService.login(form.get("name"), form.get("password"), form.get("captcha"), expected);
        oldSession.removeAttribute("captcha");
        oldSession.setAttribute("loginUser", user);
        JsonUtil.ok(resp, Dto.user(user));
    }

    private void register(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Map<String, String> form = params(req);
        User user = authService.register(form);
        JsonUtil.ok(resp, Dto.user(user));
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        JsonUtil.ok(resp, null);
    }
}