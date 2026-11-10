package com.library.dao;

import com.library.entity.BookRecommend;
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
 * 推荐图书数据访问。
 */
public class BookRecommendDao {

    private static final String SELECT_WITH_USER =
            "SELECT r.id, r.createTime, r.updateTime, r.userId, r.bookName, r.author, r.publisher, r.price, " +
            "r.status, r.reason, u.name AS userName " +
            "FROM bookrecommend r LEFT JOIN [user] u ON u.id = r.userId ";

    public void insert(Connection conn, BookRecommend recommend) throws Exception {
        JDBCTool.save(recommend, conn);
    }

    public BookRecommend findById(Connection conn, String id) throws SQLException {
        String sql = SELECT_WITH_USER + "WHERE r.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<BookRecommend> findPending(Connection conn) throws SQLException {
        String sql = SELECT_WITH_USER + "WHERE r.status = N'待审核' ORDER BY r.createTime DESC";
        return query(conn, sql);
    }

    public List<BookRecommend> findByUser(Connection conn, String userId) throws SQLException {
        String sql = SELECT_WITH_USER + "WHERE r.userId = ? ORDER BY r.createTime DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<BookRecommend> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    public void updateReview(Connection conn, String id, String status, String reason)
            throws SQLException {
        String sql = "UPDATE bookrecommend SET status = ?, reason = ?, updateTime = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, reason);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(4, id);
            ps.executeUpdate();
        }
    }

    private List<BookRecommend> query(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<BookRecommend> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        }
    }

    private BookRecommend map(ResultSet rs) throws SQLException {
        BookRecommend recommend = new BookRecommend();
        recommend.setId(rs.getString("id"));
        recommend.setCreateTime(toLocalDateTime(rs.getTimestamp("createTime")));
        recommend.setUpdateTime(toLocalDateTime(rs.getTimestamp("updateTime")));
        recommend.setUserId(rs.getString("userId"));
        recommend.setBookName(rs.getString("bookName"));
        recommend.setAuthor(rs.getString("author"));
        recommend.setPublisher(rs.getString("publisher"));
        recommend.setPrice(rs.getBigDecimal("price") == null ? null : rs.getDouble("price"));
        recommend.setStatus(rs.getString("status"));
        recommend.setReason(rs.getString("reason"));
        recommend.setUserName(rs.getString("userName"));
        return recommend;
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}