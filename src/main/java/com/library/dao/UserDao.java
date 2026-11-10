package com.library.dao;

import com.library.entity.Admin;
import com.library.entity.Operator;
import com.library.entity.Student;
import com.library.entity.Teacher;
import com.library.entity.User;
import com.util.JDBCTool;

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
 * 用户数据访问。user 是 SQL Server 保留字，SQL 中统一写作 [user]。
 */
public class UserDao {

    private static final String COLUMNS =
            "id, createTime, updateTime, name, password, phone, role, major, loginTime, " +
            "permissionLevel, entryTime, duty, area, grade, college, title";

    public List<User> search(Connection conn, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM [user]");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" WHERE name LIKE ? OR phone LIKE ? OR major LIKE ?");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY CASE WHEN createTime IS NULL THEN 1 ELSE 0 END, createTime DESC");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            JDBCTool.bind(ps, params.toArray());
            try (ResultSet rs = ps.executeQuery()) {
                List<User> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    public User findById(Connection conn, String id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM [user] WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public User findByName(Connection conn, String name) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM [user] WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean existsName(Connection conn, String name) throws SQLException {
        return exists(conn, "SELECT COUNT(*) FROM [user] WHERE name = ?", name);
    }

    public boolean existsPhone(Connection conn, String phone) throws SQLException {
        return exists(conn, "SELECT COUNT(*) FROM [user] WHERE phone = ?", phone);
    }

    private boolean exists(Connection conn, String sql, String value) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void insert(Connection conn, User user) throws Exception {
        JDBCTool.save(user, conn);
    }

    public void updateLoginTime(Connection conn, String userId, LocalDateTime loginTime) throws SQLException {
        String sql = "UPDATE [user] SET loginTime = ?, updateTime = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(loginTime));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, userId);
            ps.executeUpdate();
        }
    }

    public List<String> listMajors(Connection conn) throws SQLException {
        String sql = "SELECT DISTINCT major FROM [user] WHERE major IS NOT NULL AND major <> '' ORDER BY major";
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString(1));
            }
        }
        return list;
    }

    private User map(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
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
        user.setId(rs.getString("id"));
        user.setCreateTime(toLocalDateTime(rs.getTimestamp("createTime")));
        user.setUpdateTime(toLocalDateTime(rs.getTimestamp("updateTime")));
        user.setName(rs.getString("name"));
        user.setPassword(rs.getString("password"));
        user.setPhone(rs.getString("phone"));
        user.setRole(role);
        user.setMajor(rs.getString("major"));
        user.setLoginTime(toLocalDateTime(rs.getTimestamp("loginTime")));

        if (user instanceof Admin) {
            Admin admin = (Admin) user;
            Integer level = getInteger(rs, "permissionLevel");
            admin.setPermissionLevel(level);
            java.sql.Date entry = rs.getDate("entryTime");
            admin.setEntryTime(entry == null ? null : entry.toLocalDate());
        } else if (user instanceof Operator) {
            Operator operator = (Operator) user;
            operator.setArea(rs.getString("area"));
            operator.setDuty(rs.getString("duty"));
        } else if (user instanceof Student) {
            Student student = (Student) user;
            student.setGrade(rs.getString("grade"));
            student.setCollege(rs.getString("college"));
        } else if (user instanceof Teacher) {
            Teacher teacher = (Teacher) user;
            teacher.setCollege(rs.getString("college"));
            teacher.setTitle(rs.getString("title"));
        }
        return user;
    }

    private static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    static LocalDate toLocalDate(java.sql.Date date) {
        return date == null ? null : date.toLocalDate();
    }
}