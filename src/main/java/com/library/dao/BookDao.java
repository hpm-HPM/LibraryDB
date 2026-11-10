package com.library.dao;

import com.library.entity.Book;
import com.util.JDBCTool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 图书数据访问。所有查询都不读取 embedding 大字段。
 */
public class BookDao {

    private static final String COLUMNS =
            "id, createTime, updateTime, title, author, type, publisher, days, price, stock, " +
            "status, introduction, tags";

    public List<Book> search(Connection conn, String keyword) throws Exception {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM book");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" WHERE title LIKE ? OR author LIKE ? OR type LIKE ? OR publisher LIKE ?");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY CASE WHEN createTime IS NULL THEN 1 ELSE 0 END, createTime DESC, title ASC");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            JDBCTool.bind(ps, params.toArray());
            try (ResultSet rs = ps.executeQuery()) {
                return JDBCTool.resultSetToList(rs, Book.class);
            }
        }
    }

    /**
     * 借书页面只展示状态正常且可借库存大于 0 的图书。
     */
    public List<Book> listBorrowable(Connection conn) throws Exception {
        String sql = "SELECT " + COLUMNS + " FROM book WHERE status = N'正常' AND stock > 0 " +
                "ORDER BY title ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return JDBCTool.resultSetToList(rs, Book.class);
        }
    }

    public Book findById(Connection conn, String id) throws Exception {
        String sql = "SELECT " + COLUMNS + " FROM book WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<Book> list = JDBCTool.resultSetToList(rs, Book.class);
                return list.isEmpty() ? null : list.get(0);
            }
        }
    }

    /**
     * 加行锁读取图书，用于借书 / 废弃 / 副本变更事务。
     */
    public Book findByIdForUpdate(Connection conn, String id) throws Exception {
        String sql = "SELECT " + COLUMNS + " FROM book WITH (UPDLOCK, ROWLOCK) WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<Book> list = JDBCTool.resultSetToList(rs, Book.class);
                return list.isEmpty() ? null : list.get(0);
            }
        }
    }

    public List<Book> findByTitleLike(Connection conn, String title) throws Exception {
        String sql = "SELECT TOP 10 " + COLUMNS + " FROM book WHERE title LIKE ? ORDER BY title";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + title + "%");
            try (ResultSet rs = ps.executeQuery()) {
                return JDBCTool.resultSetToList(rs, Book.class);
            }
        }
    }

    public void insert(Connection conn, Book book) throws Exception {
        JDBCTool.save(book, conn);
    }

    /**
     * 编辑图书：只更新题名、作者、类型、出版社、可借天数、价格六项，
     * 不更新 introduction / tags / stock / status / bookCopies。
     */
    public void updateEditable(Connection conn, Book book) throws SQLException {
        String sql = "UPDATE book SET title = ?, author = ?, type = ?, publisher = ?, days = ?, price = ?, " +
                "updateTime = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getType());
            ps.setString(4, book.getPublisher());
            ps.setObject(5, book.getDays());
            ps.setObject(6, book.getPrice());
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(8, book.getId());
            ps.executeUpdate();
        }
    }

    /**
     * 废弃图书：仅改状态，保留借阅记录和副本。
     */
    public void discard(Connection conn, String bookId) throws SQLException {
        String sql = "UPDATE book SET status = N'已废弃', updateTime = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, bookId);
            ps.executeUpdate();
        }
    }

    /**
     * 调整可借库存，delta 为 -1（借出）或 +1（归还 / 新增副本）。
     */
    public int adjustStock(Connection conn, String bookId, int delta) throws SQLException {
        String sql = "UPDATE book SET stock = stock + ?, updateTime = ? WHERE id = ? AND stock + ? >= 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, bookId);
            ps.setInt(4, delta);
            return ps.executeUpdate();
        }
    }

    public int countCopies(Connection conn, String bookId) throws SQLException {
        return count(conn, "SELECT COUNT(*) FROM bookcopy WHERE bookId = ?", bookId);
    }

    public int countBorrowedCopies(Connection conn, String bookId) throws SQLException {
        return count(conn, "SELECT COUNT(*) FROM bookcopy WHERE bookId = ? AND isBorrowed = '1'", bookId);
    }

    private int count(Connection conn, String sql, String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<String> listTypes(Connection conn) throws SQLException {
        String sql = "SELECT DISTINCT type FROM book WHERE type IS NOT NULL AND type <> '' ORDER BY type";
        List<String> types = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                types.add(rs.getString(1));
            }
        }
        return types;
    }
}