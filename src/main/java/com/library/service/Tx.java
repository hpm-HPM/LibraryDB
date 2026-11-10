package com.library.service;

import com.util.JDBCTool;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 简单事务模板：开启事务、提交、异常回滚，避免每个业务方法重复 try/catch。
 */
final class Tx {

    @FunctionalInterface
    interface Work<T> {
        T run(Connection conn) throws Exception;
    }

    private Tx() {
    }

    static <T> T execute(Work<T> work) throws Exception {
        try (Connection conn = JDBCTool.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.run(conn);
                conn.commit();
                return result;
            } catch (Exception e) {
                JDBCTool.rollbackQuietly(conn);
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }
}