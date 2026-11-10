package com.library.servlet;

import com.library.entity.Loan;
import com.library.entity.User;
import com.library.service.LoanService;
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
 * 借书、还书、个人借阅查询、管理员借阅记录查询。
 */
@WebServlet("/api/loans/*")
public class LoanServlet extends BaseServlet {

    private final LoanService loanService = new LoanService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String action = req.getPathInfo();
        if ("/my".equals(action)) {
            User user = requireLogin(req);
            List<Loan> loans = loanService.myLoans(user, req.getParameter("filter"),
                    req.getParameter("start"), req.getParameter("end"));
            JsonUtil.ok(resp, toData(loans));
        } else if ("/search".equals(action)) {
            requirePermission(req, "借阅记录查询");
            List<Loan> loans = loanService.search(req.getParameter("mode"), req.getParameter("keyword"),
                    req.getParameter("start"), req.getParameter("end"));
            JsonUtil.ok(resp, toData(loans));
        } else {
            notFound();
        }
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String action = req.getPathInfo() == null ? "" : req.getPathInfo();
        if ("/borrow".equals(action)) {
            User user = requirePermission(req, "借书");
            Loan loan = loanService.borrow(user, req.getParameter("bookId"));
            JsonUtil.ok(resp, Dto.loan(loan));
        } else if ("/return".equals(action)) {
            User user = requirePermission(req, "还书");
            Loan loan = loanService.returnBook(user, req.getParameter("copyId"));
            JsonUtil.ok(resp, Dto.loan(loan));
        } else {
            notFound();
        }
    }

    private List<Map<String, Object>> toData(List<Loan> loans) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (Loan loan : loans) {
            data.add(Dto.loan(loan));
        }
        return data;
    }
}