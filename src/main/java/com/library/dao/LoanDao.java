package com.library.dao;

import com.library.entity.Admin;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.Loan;
import com.library.entity.Operator;
import com.library.entity.Student;
import com.library.entity.Teacher;
import com.library.entity.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 借阅记录数据访问，联表查询需要手工映射 Loan 中的嵌套对象。
 */
public class LoanDao {

    private static final String SELECT_COLUMNS =
            "l.id, l.createTime, l.updateTime, l.borrowDate, l.returnDate, l.dueDate, l.status, " +
            "c.id AS copyId, c.isBorrowed AS copyBorrowed, " +
            "b.id AS bookId, b.title AS bookTitle, b.author AS bookAuthor, b.type AS bookType, b.days AS bookDays, " +
            "u.id AS userId, u.name AS userName, u.role AS userRole, u.major AS userMajor";

    private static final String FROM_JOINS =
            " FROM loan l " +
            "JOIN bookcopy c ON c.id = l.bookCopyId " +
            "JOIN book b ON b.id = c.bookId " +
            "JOIN [user] u ON u.id = l.userId";

    private static final String BASE = "SELECT " + SELECT_COLUMNS + FROM_JOINS;

    public void insert(Connection conn, Loan loan) throws SQLException {
        String sql = "INSERT INTO loan (id, createTime, updateTime, bookCopyId, userId, borrowDate, returnDate, dueDate, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loan.getId());
            ps.setTimestamp(2, Timestamp.valueOf(loan.getCreateTime()));
            ps.setTimestamp(3, Timestamp.valueOf(loan.getUpdateTime()));
            ps.setString(4, loan.getBookCopy().getId());
            ps.setString(5, loan.getUser().getId());
            ps.setTimestamp(6, Timestamp.valueOf(loan.getBorrowDate()));
            ps.setTimestamp(7, loan.getReturnDate() == null ? null : Timestamp.valueOf(loan.getReturnDate()));
            ps.setTimestamp(8, Timestamp.valueOf(loan.getDueDate()));
            ps.setString(9, loan.getStatus());
            ps.executeUpdate();
        }
    }

    public Loan findById(Connection conn, String id) throws SQLException {
        String sql = BASE + " WHERE l.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * 统计某用户当前尚未归还的借阅数量（用于限制同时在借数量）。
     */
    public int countActiveByUser(Connection conn, String userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM loan WHERE userId = ? AND returnDate IS NULL AND status <> N'已还'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * 按副本查询尚未归还的借阅记录（还书时使用）。
     */
    public Loan findActiveByCopyId(Connection conn, String copyId) throws SQLException {
        String sql = "SELECT TOP 1 " + SELECT_COLUMNS + FROM_JOINS +
                " WHERE c.id = ? AND l.returnDate IS NULL AND l.status <> N'已还' " +
                "ORDER BY l.borrowDate DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, copyId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Loan> findByUser(Connection conn, String userId, boolean onlyUnreturned,
                                 LocalDate start, LocalDate end) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE l.userId = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (onlyUnreturned) {
            sql.append(" AND l.returnDate IS NULL AND l.status <> N'已还'");
        }
        appendDateRange(sql, params, start, end);
        sql.append(" ORDER BY l.borrowDate DESC, l.createTime DESC");
        return query(conn, sql.toString(), params);
    }

    /**
     * @param mode keyword（按用户名或书名模糊查询）、overdue（超期未还）、range（借阅日期范围）
     */
    public List<Loan> search(Connection conn, String mode, String keyword,
                             LocalDate start, LocalDate end) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        String like = keyword == null ? null : "%" + keyword.trim() + "%";
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

        if ("overdue".equals(mode)) {
            sql.append(" AND l.dueDate < GETDATE() AND l.returnDate IS NULL AND l.status <> N'已还'");
            if (hasKeyword) {
                sql.append(" AND (u.name LIKE ? OR b.title LIKE ?)");
                params.add(like);
                params.add(like);
            }
        } else if ("range".equals(mode)) {
            appendDateRange(sql, params, start, end);
            if (hasKeyword) {
                sql.append(" AND (u.name LIKE ? OR b.title LIKE ?)");
                params.add(like);
                params.add(like);
            }
        } else {
            if (hasKeyword) {
                sql.append(" AND (u.name LIKE ? OR b.title LIKE ?)");
                params.add(like);
                params.add(like);
            }
        }
        sql.append(" ORDER BY l.borrowDate DESC, l.createTime DESC");
        return query(conn, sql.toString(), params);
    }

    private void appendDateRange(StringBuilder sql, List<Object> params, LocalDate start, LocalDate end) {
        if (start != null) {
            sql.append(" AND l.borrowDate >= ?");
            params.add(start.atStartOfDay());
        }
        if (end != null) {
            sql.append(" AND l.borrowDate < ?");
            params.add(end.plusDays(1).atStartOfDay());
        }
    }

    private List<Loan> query(Connection conn, String sql, List<Object> params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            com.util.JDBCTool.bind(ps, params.toArray());
            try (ResultSet rs = ps.executeQuery()) {
                List<Loan> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    public void markReturned(Connection conn, String loanId, LocalDateTime returnTime) throws SQLException {
        String sql = "UPDATE loan SET returnDate = ?, status = N'已还', updateTime = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(returnTime));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, loanId);
            ps.executeUpdate();
        }
    }

    private Loan map(ResultSet rs) throws SQLException {
        Loan loan = new Loan();
        loan.setId(rs.getString("id"));
        loan.setCreateTime(toLocalDateTime(rs.getTimestamp("createTime")));
        loan.setUpdateTime(toLocalDateTime(rs.getTimestamp("updateTime")));
        loan.setBorrowDate(toLocalDateTime(rs.getTimestamp("borrowDate")));
        loan.setReturnDate(toLocalDateTime(rs.getTimestamp("returnDate")));
        loan.setDueDate(toLocalDateTime(rs.getTimestamp("dueDate")));
        loan.setStatus(rs.getString("status"));

        Book book = new Book();
        book.setId(rs.getString("bookId"));
        book.setTitle(rs.getString("bookTitle"));
        book.setAuthor(rs.getString("bookAuthor"));
        book.setType(rs.getString("bookType"));
        book.setDays(getInteger(rs, "bookDays"));

        BookCopy copy = new BookCopy();
        copy.setId(rs.getString("copyId"));
        copy.setBorrowed("1".equals(rs.getString("copyBorrowed")));
        copy.setBook(book);
        loan.setBookCopy(copy);

        String role = rs.getString("userRole");
        User user;
        if ("管理员".equals(role)) {
            user = new Admin();
        } else if ("操作员".equals(role)) {
            user = new Operator();
        } else if ("教师".equals(role)) {
            user = new Teacher();
        } else {
            user = new Student();
        }
        user.setId(rs.getString("userId"));
        user.setName(rs.getString("userName"));
        user.setRole(role);
        user.setMajor(rs.getString("userMajor"));
        loan.setUser(user);
        return loan;
    }

    private static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}