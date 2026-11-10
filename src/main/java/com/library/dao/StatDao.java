package com.library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 借阅统计。两类统计均使用 LEFT JOIN，保证没有借阅记录的图书类型 / 专业也会显示。
 */
public class StatDao {

    /**
     * 按图书类型统计借阅次数。
     */
    public List<Map<String, Object>> countByBookType(Connection conn) throws SQLException {
        String sql = "SELECT ISNULL(NULLIF(b.type, ''), N'未分类') AS name, COUNT(l.id) AS cnt " +
                "FROM book b " +
                "LEFT JOIN bookcopy c ON c.bookId = b.id " +
                "LEFT JOIN loan l ON l.bookCopyId = c.id " +
                "GROUP BY ISNULL(NULLIF(b.type, ''), N'未分类') " +
                "ORDER BY cnt DESC, name ASC";
        return query(conn, sql);
    }

    /**
     * 按用户专业统计借阅次数。
     */
    public List<Map<String, Object>> countByUserMajor(Connection conn) throws SQLException {
        String sql = "SELECT ISNULL(NULLIF(u.major, ''), N'未填写') AS name, COUNT(l.id) AS cnt " +
                "FROM [user] u " +
                "LEFT JOIN loan l ON l.userId = u.id " +
                "GROUP BY ISNULL(NULLIF(u.major, ''), N'未填写') " +
                "ORDER BY cnt DESC, name ASC";
        return query(conn, sql);
    }

    private List<Map<String, Object>> query(Connection conn, String sql) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", rs.getString("name"));
                row.put("count", rs.getInt("cnt"));
                list.add(row);
            }
        }
        return list;
    }
}