package com.library.servlet;

import com.library.entity.BookCopy;
import com.library.service.BookService;
import com.library.service.BusinessException;
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
 * 图书副本管理（操作员）。
 */
@WebServlet("/api/bookcopies/*")
public class BookCopyServlet extends BaseServlet {

    private final BookService bookService = new BookService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        requirePermission(req, "图书副本编辑");
        String bookId = req.getParameter("bookId");
        if (bookId == null || bookId.trim().isEmpty()) {
            throw new BusinessException("图书ID不能为空");
        }
        List<Map<String, Object>> data = new ArrayList<>();
        for (BookCopy copy : bookService.listCopies(bookId.trim())) {
            data.add(Dto.bookCopy(copy));
        }
        JsonUtil.ok(resp, data);
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String action = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/add".equals(action)) {
            requirePermission(req, "图书副本编辑");
            BookCopy copy = bookService.addCopy(req.getParameter("bookId"));
            JsonUtil.ok(resp, Dto.bookCopy(copy));
        } else if ("/delete".equals(action)) {
            requirePermission(req, "图书副本编辑");
            bookService.deleteCopy(req.getParameter("id"));
            JsonUtil.ok(resp, null);
        } else {
            notFound();
        }
    }
}