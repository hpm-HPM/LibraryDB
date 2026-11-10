package com.library.servlet;

import com.library.dao.StatDao;
import com.util.JDBCTool;
import com.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;

/**
 * 借阅数据统计（管理员）：按图书类型、按用户专业。
 */
@WebServlet("/api/stats/*")
public class StatServlet extends BaseServlet {

    private final StatDao statDao = new StatDao();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        requirePermission(req, "借阅数据统计");
        String action = req.getPathInfo();
        if (!"/types".equals(action) && !"/majors".equals(action)) {
            notFound();
        }
        try (Connection conn = JDBCTool.getConnection()) {
            if ("/types".equals(action)) {
                JsonUtil.ok(resp, statDao.countByBookType(conn));
            } else {
                JsonUtil.ok(resp, statDao.countByUserMajor(conn));
            }
        }
    }
}