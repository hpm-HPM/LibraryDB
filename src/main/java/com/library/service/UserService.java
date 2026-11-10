package com.library.service;

import com.library.dao.UserDao;
import com.library.entity.User;
import com.util.JDBCTool;

import java.sql.Connection;
import java.util.List;

/**
 * 用户查询业务（仅管理员使用）。
 */
public class UserService {

    private final UserDao userDao = new UserDao();

    public List<User> search(String keyword) throws Exception {
        try (Connection conn = JDBCTool.getConnection()) {
            return userDao.search(conn, keyword);
        }
    }

    public User detail(String id) throws Exception {
        try (Connection conn = JDBCTool.getConnection()) {
            return userDao.findById(conn, id);
        }
    }
}