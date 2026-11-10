package com.library.service;

import com.library.dao.BookCopyDao;
import com.library.dao.BookDao;
import com.library.dao.LoanDao;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.Loan;
import com.library.entity.User;
import com.util.JDBCTool;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 借书、还书与借阅记录查询。
 */
public class LoanService {

    private final BookDao bookDao = new BookDao();
    private final BookCopyDao copyDao = new BookCopyDao();
    private final LoanDao loanDao = new LoanDao();

    /**
     * 管理员按关键字 / 超期 / 借阅日期范围查询全部借阅记录。
     */
    public List<Loan> search(String mode, String keyword, String start, String end) throws Exception {
        String queryMode = mode == null ? "keyword" : mode.trim();
        if (!"keyword".equals(queryMode) && !"overdue".equals(queryMode) && !"range".equals(queryMode)) {
            queryMode = "keyword";
        }
        LocalDate begin = parseDate(start, "开始日期");
        LocalDate finish = parseDate(end, "结束日期");
        checkRange(begin, finish);
        try (Connection conn = JDBCTool.getConnection()) {
            return loanDao.search(conn, queryMode, keyword, begin, finish);
        }
    }

    /**
     * 个人借阅查询：filter=all|unreturned，可选借阅日期范围。
     */
    public List<Loan> myLoans(User user, String filter, String start, String end) throws Exception {
        boolean onlyUnreturned = "unreturned".equalsIgnoreCase(filter) || "未还".equals(filter);
        LocalDate begin = parseDate(start, "开始日期");
        LocalDate finish = parseDate(end, "结束日期");
        checkRange(begin, finish);
        try (Connection conn = JDBCTool.getConnection()) {
            return loanDao.findByUser(conn, user.getId(), onlyUnreturned, begin, finish);
        }
    }

    /**
     * 借书：按图书 ID 借出一本可借副本，库存 -1。
     */
    public Loan borrow(User user, String bookId) throws Exception {
        String id = ServiceSupport.requireText(bookId, "图书ID");
        return Tx.execute(conn -> {
            Book book = bookDao.findByIdForUpdate(conn, id);
            if (book == null) {
                throw new BusinessException("图书不存在");
            }
            if (!"正常".equals(book.getStatus())) {
                throw new BusinessException("该图书已废弃，不能借阅");
            }
            if (book.getStock() == null || book.getStock() <= 0) {
                throw new BusinessException("该图书暂无可借副本");
            }
            if (loanDao.countActiveByUser(conn, user.getId()) >= 5) {
                throw new BusinessException("每位读者最多同时借阅 5 本，请先归还部分图书");
            }
            BookCopy copy = copyDao.lockAvailableCopy(conn, id);
            if (copy == null) {
                throw new BusinessException("该图书暂无可借副本");
            }
            copyDao.setBorrowed(conn, copy.getId(), true);
            if (bookDao.adjustStock(conn, id, -1) == 0) {
                throw new BusinessException("借阅失败，图书库存不足");
            }
            book.setStock(book.getStock() - 1);
            int days = book.getDays() == null || book.getDays() <= 0 ? 7 : book.getDays();
            LocalDateTime now = LocalDateTime.now();
            copy.setBorrowed(true);
            copy.setBook(book);

            Loan loan = new Loan();
            ServiceSupport.initBase(loan);
            loan.setBookCopy(copy);
            loan.setUser(user);
            loan.setBorrowDate(now);
            loan.setDueDate(now.plusDays(days));
            loan.setStatus("未还");
            loanDao.insert(conn, loan);
            return loan;
        });
    }

    /**
     * 还书：按副本 ID 归还本人尚未归还的借阅，库存 +1。
     */
    public Loan returnBook(User user, String copyId) throws Exception {
        String id = ServiceSupport.requireText(copyId, "副本ID");
        return Tx.execute(conn -> {
            BookCopy copy = copyDao.findById(conn, id);
            if (copy == null) {
                throw new BusinessException("图书副本不存在");
            }
            Book book = bookDao.findByIdForUpdate(conn, copy.getBook().getId());
            if (book == null) {
                throw new BusinessException("副本对应的图书不存在");
            }
            Loan loan = loanDao.findActiveByCopyId(conn, id);
            if (loan == null) {
                throw new BusinessException("该副本没有未归还的借阅记录");
            }
            if (loan.getUser() == null || !user.getId().equals(loan.getUser().getId())) {
                throw new BusinessException("只能归还自己借阅的图书");
            }
            copyDao.setBorrowed(conn, id, false);
            if (bookDao.adjustStock(conn, book.getId(), 1) == 0) {
                throw new BusinessException("归还失败，图书库存异常");
            }
            LocalDateTime now = LocalDateTime.now();
            loanDao.markReturned(conn, loan.getId(), now);
            // 同步内存对象，保证接口响应与数据库一致
            copy.setBorrowed(false);
            copy.setBook(book);
            book.setStock((book.getStock() == null ? 0 : book.getStock()) + 1);
            loan.setBookCopy(copy);
            loan.setReturnDate(now);
            loan.setStatus("已还");
            return loan;
        });
    }

    private LocalDate parseDate(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException(label + "格式应为 yyyy-MM-dd");
        }
    }

    private void checkRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
    }
}