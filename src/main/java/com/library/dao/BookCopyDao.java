package com.library.dao;

import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.util.JDBCTool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 图书副本数据访问。
 */
public class BookCopyDao {

    private static final String SELECT_WITH_BOOK =
            "SELECT c.id, c.createTime, c.updateTime, c.isBorrowed, " +
            "b.id AS bookId, b.title AS bookTitle, b.author AS bookAuthor, b.type AS bookType, b.days AS bookDays " +
            "FROM bookcopy c JOIN book b ON b.id = c.bookId ";

    public List<BookCopy> findByBook(Connection conn, String bookId) throws SQLException {
        String sql = SELECT_WITH_BOOK + "WHERE c.bookId = ? ORDER BY c.createTime ASC, c.id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                List<BookCopy> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    public BookCopy findById(Connection conn, String copyId) throws SQLException {
        String sql = SELECT_WITH_BOOK + "WHERE c.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, copyId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * 锁定一条可借副本（事务内调用），防止两个用户借到同一副本。
     */
    public BookCopy lockAvailableCopy(Connection conn, String bookId) throws SQLException {
        String sql = "SELECT TOP 1 id, createTime, updateTime, isBorrowed, bookId " +
                "FROM bookcopy WITH (UPDLOCK, ROWLOCK) " +
                "WHERE bookId = ? AND isBorrowed = '0' ORDER BY createTime ASC, id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                BookCopy copy = new BookCopy();
                copy.setId(rs.getString("id"));
                copy.setCreateTime(toLocalDateTime(rs.getTimestamp("createTime")));
                copy.setUpdateTime(toLocalDateTime(rs.getTimestamp("updateTime")));
                copy.setBorrowed("1".equals(rs.getString("isBorrowed")));
                Book book = new Book();
                book.setId(rs.getString("bookId"));
                copy.setBook(book);
                return copy;
            }
        }
    }

    /**
     * 新增副本。id / createTime 由调用方准备，缺省时这里补全。
     */
    public void insert(Connection conn, BookCopy copy, String bookId) throws SQLException {
        if (copy.getId() == null) {
            copy.setId(UUID.randomUUID().toString());
        }
        LocalDateTime now = LocalDateTime.now();
        if (copy.getCreateTime() == null) {
            copy.setCreateTime(now);
        }
        if (copy.getUpdateTime() == null) {
            copy.setUpdateTime(now);
        }
        String sql = "INSERT INTO bookcopy (id, createTime, updateTime, bookId, isBorrowed) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, copy.getId());
            ps.setTimestamp(2, Timestamp.valueOf(copy.getCreateTime()));
            ps.setTimestamp(3, Timestamp.valueOf(copy.getUpdateTime()));
            ps.setString(4, bookId);
            ps.setString(5, copy.isBorrowed() ? "1" : "0");
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, String copyId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM bookcopy WHERE id = ?")) {
            ps.setString(1, copyId);
            ps.executeUpdate();
        }
    }

    public void setBorrowed(Connection conn, String copyId, boolean borrowed) throws SQLException {
        String sql = "UPDATE bookcopy SET isBorrowed = ?, updateTime = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, borrowed ? "1" : "0");
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, copyId);
            ps.executeUpdate();
        }
    }

    public int countByBook(Connection conn, String bookId) throws SQLException {
        return count(conn, "SELECT COUNT(*) FROM bookcopy WHERE bookId = ?", bookId);
    }

    public int countBorrowedByBook(Connection conn, String bookId) throws SQLException {
        return count(conn, "SELECT COUNT(*) FROM bookcopy WHERE bookId = ? AND isBorrowed = '1'", bookId);
    }

    public int countLoans(Connection conn, String copyId) throws SQLException {
        return count(conn, "SELECT COUNT(*) FROM loan WHERE bookCopyId = ?", copyId);
    }

    private int count(Connection conn, String sql, String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private BookCopy map(ResultSet rs) throws SQLException {
        BookCopy copy = new BookCopy();
        copy.setId(rs.getString("id"));
        copy.setCreateTime(toLocalDateTime(rs.getTimestamp("createTime")));
        copy.setUpdateTime(toLocalDateTime(rs.getTimestamp("updateTime")));
        copy.setBorrowed("1".equals(rs.getString("isBorrowed")));
        Book book = new Book();
        book.setId(rs.getString("bookId"));
        book.setTitle(rs.getString("bookTitle"));
        book.setAuthor(rs.getString("bookAuthor"));
        book.setType(rs.getString("bookType"));
        book.setDays(getInteger(rs, "bookDays"));
        copy.setBook(book);
        return copy;
    }

    private static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}