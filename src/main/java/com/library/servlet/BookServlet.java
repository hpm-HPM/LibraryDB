package com.library.servlet;

import com.library.entity.Book;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 图书查询、添加、编辑、废弃。
 */
@WebServlet("/api/books/*")
public class BookServlet extends BaseServlet {

    private final BookService bookService = new BookService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String action = req.getPathInfo();
        if (action == null || "/".equals(action) || "/search".equals(action)) {
            search(req, resp);
        } else if ("/borrowable".equals(action)) {
            List<Map<String, Object>> data = new ArrayList<>();
            for (Book book : bookService.listBorrowable()) {
                data.add(Dto.book(book));
            }
            JsonUtil.ok(resp, data);
        } else if ("/types".equals(action)) {
            JsonUtil.ok(resp, bookService.listTypes());
        } else if ("/detail".equals(action)) {
            detail(req, resp);
        } else {
            notFound();
        }
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String action = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/add".equals(action)) {
            requirePermission(req, "添加书籍");
            Book book = bookService.add(params(req));
            JsonUtil.ok(resp, Dto.book(book));
        } else if ("/update".equals(action)) {
            requirePermission(req, "编辑图书");
            bookService.update(params(req));
            JsonUtil.ok(resp, null);
        } else if ("/discard".equals(action)) {
            requirePermission(req, "废弃图书");
            bookService.discard(req.getParameter("id"));
            JsonUtil.ok(resp, null);
        } else {
            notFound();
        }
    }

    private void search(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        requireLogin(req);
        String keyword = req.getParameter("keyword");
        List<Map<String, Object>> data = new ArrayList<>();
        for (Book book : bookService.search(keyword)) {
            data.add(Dto.book(book));
        }
        JsonUtil.ok(resp, data);
    }

    private void detail(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        requireLogin(req);
        String id = req.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            throw new BusinessException("图书ID不能为空");
        }
        Book book = bookService.detail(id.trim());
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        List<Map<String, Object>> copies = new ArrayList<>();
        for (BookCopy copy : bookService.listCopies(id.trim())) {
            copies.add(Dto.bookCopy(copy));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("book", Dto.book(book));
        data.put("copies", copies);
        data.put("copyCount", bookService.countCopies(id.trim()));
        data.put("borrowedCount", bookService.countBorrowedCopies(id.trim()));
        JsonUtil.ok(resp, data);
    }
}