package com.library.servlet;

import com.library.entity.BookRecommend;
import com.library.entity.User;
import com.library.service.RecommendService;
import com.util.Dto;
import com.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 教师推荐图书、管理员审阅推荐。
 */
@WebServlet("/api/recommend/*")
public class RecommendServlet extends BaseServlet {

    private final RecommendService recommendService = new RecommendService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String action = req.getPathInfo();
        if ("/mine".equals(action)) {
            User user = requirePermission(req, "推荐图书");
            JsonUtil.ok(resp, toData(recommendService.mine(user)));
        } else if ("/pending".equals(action)) {
            requirePermission(req, "审阅推荐图书");
            JsonUtil.ok(resp, toData(recommendService.pending()));
        } else {
            notFound();
        }
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String action = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/check".equals(action)) {
            User user = requirePermission(req, "推荐图书");
            Map<String, Object> result = recommendService.check(user, params(req));
            JsonUtil.ok(resp, result);
        } else if ("/review".equals(action)) {
            User reviewer = requirePermission(req, "审阅推荐图书");
            recommendService.review(reviewer, params(req));
            JsonUtil.ok(resp, null);
        } else {
            notFound();
        }
    }

    private List<Map<String, Object>> toData(List<BookRecommend> recommends) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (BookRecommend recommend : recommends) {
            data.add(Dto.recommend(recommend));
        }
        return data;
    }
}