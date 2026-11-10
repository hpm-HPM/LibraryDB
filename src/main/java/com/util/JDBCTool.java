package com.util;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用 JDBC 工具类（在作业1 JDBCTool 的基础上增强）。
 *
 * <p>保留作业1 的主要 API：getConnection / close / resultSetToList /
 * save / update / delete / getOneById，并增加：
 * <ul>
 *   <li>读取 classpath 下的 db.properties（可用 -Dlibrary.db.* 覆盖，便于测试）；</li>
 *   <li>反射支持继承字段、LocalDateTime / LocalDate / boolean 等类型转换；</li>
 *   <li>通过 {@link TableName} 支持 [user] 等保留字表名，默认类名小写；</li>
 *   <li>跳过集合、实体、static、transient 字段，避免 Book.bookCopies 等字段参与 SQL。</li>
 * </ul>
 * 所有表名均使用方括号包裹，兼容 SQL Server 保留字。
 */
public class JDBCTool {

    private static String url;
    private static String username;
    private static String password;
    private static volatile boolean configLoaded = false;

    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    private JDBCTool() {
    }

    /* ==================== 连接配置 ==================== */

    private static synchronized void loadConfig() {
        if (configLoaded) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = JDBCTool.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            throw new IllegalStateException("读取数据库配置文件 db.properties 失败", e);
        }
        url = System.getProperty("library.db.url", props.getProperty("jdbc.url"));
        username = System.getProperty("library.db.username", props.getProperty("jdbc.username"));
        password = System.getProperty("library.db.password", props.getProperty("jdbc.password"));
        if (url == null || username == null) {
            throw new IllegalStateException("数据库连接配置缺失，请检查 src/main/resources/db.properties");
        }
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException ignored) {
            // 新版 JDBC 驱动可通过 SPI 自动加载
        }
        configLoaded = true;
    }

    public static Connection getConnection() throws SQLException {
        loadConfig();
        return DriverManager.getConnection(url, username, password);
    }

    /* ==================== 关闭资源 ==================== */

    public static void close(ResultSet rs, Statement st, Connection conn) {
        close(rs);
        close(st);
        close(conn);
    }

    public static void close(ResultSet rs, Statement st) {
        close(rs);
        close(st);
    }

    public static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public static void close(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public static void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
        }
    }

    /* ==================== 反射元数据 ==================== */

    /**
     * 取得实体类（含父类）中所有需要映射到数据库列的字段。
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, c -> {
            List<Class<?>> chain = new ArrayList<>();
            for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
                chain.add(0, k);
            }
            List<Field> fields = new ArrayList<>();
            for (Class<?> k : chain) {
                for (Field f : k.getDeclaredFields()) {
                    int mod = f.getModifiers();
                    if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || f.isSynthetic()) {
                        continue;
                    }
                    if (Collection.class.isAssignableFrom(f.getType())
                            || Map.class.isAssignableFrom(f.getType())) {
                        continue;
                    }
                    if (!isColumnType(f.getType())) {
                        continue;
                    }
                    f.setAccessible(true);
                    fields.add(f);
                }
            }
            return fields;
        });
    }

    private static boolean isColumnType(Class<?> type) {
        return type == String.class
                || type == Integer.class || type == int.class
                || type == Long.class || type == long.class
                || type == Short.class || type == short.class
                || type == Byte.class || type == byte.class
                || type == Double.class || type == double.class
                || type == Float.class || type == float.class
                || type == Boolean.class || type == boolean.class
                || type == LocalDate.class || type == LocalDateTime.class
                || type == Date.class || type == java.sql.Date.class || type == Timestamp.class
                || type == BigDecimal.class
                || type.isEnum();
    }

    /**
     * 实体类对应的表名（带方括号）。
     */
    public static String tableName(Class<?> clazz) {
        TableName annotation = clazz.getAnnotation(TableName.class);
        String name = annotation != null ? annotation.value() : clazz.getSimpleName().toLowerCase();
        return "[" + name + "]";
    }

    /* ==================== 查询 ==================== */

    /**
     * 将 ResultSet 的当前结果集映射为实体列表。列名与字段名忽略大小写匹配，
     * 结果集中不存在的列会被跳过（例如联表查询只取部分列）。
     */
    public static <T> List<T> resultSetToList(ResultSet rs, Class<T> clazz) throws Exception {
        List<T> list = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            columns.put(meta.getColumnLabel(i).toLowerCase(), i);
        }
        List<Field> fields = getAllFields(clazz);
        while (rs.next()) {
            T obj = clazz.getDeclaredConstructor().newInstance();
            for (Field field : fields) {
                Integer index = columns.get(field.getName().toLowerCase());
                if (index == null) {
                    continue;
                }
                Object value = rs.getObject(index);
                field.set(obj, convert(value, field.getType()));
            }
            list.add(obj);
        }
        return list;
    }

    /**
     * 通用查询：SELECT 语句返回实体列表。表名等 SQL 由调用方保证安全。
     */
    public static <T> List<T> queryList(String sql, Class<T> clazz, Object... params) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return resultSetToList(rs, clazz);
            }
        }
    }

    /**
     * 按主键查询单条记录（SELECT *）。
     */
    public static <T> T getOneById(String id, Class<T> clazz, Connection conn) throws Exception {
        String sql = "SELECT * FROM " + tableName(clazz) + " WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> list = resultSetToList(rs, clazz);
                return list.isEmpty() ? null : list.get(0);
            }
        }
    }

    /**
     * 查询单个标量值。
     */
    public static Object queryScalar(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1) : null;
            }
        }
    }

    /* ==================== 增删改 ==================== */

    public static <T> int save(T obj, Connection conn) throws Exception {
        if (obj == null) {
            throw new IllegalArgumentException("save 对象不能为空");
        }
        Class<?> clazz = obj.getClass();
        List<Field> fields = getAllFields(clazz);
        List<Field> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Field field : fields) {
            Object value = field.get(obj);
            String name = field.getName();
            if ("id".equals(name) && value == null) {
                value = UUID.randomUUID().toString();
                field.set(obj, value);
            }
            if (("createTime".equals(name) || "updateTime".equals(name)) && value == null) {
                value = LocalDateTime.now();
                field.set(obj, value);
            }
            columns.add(field);
            values.add(value);
        }
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("实体没有任何可保存的字段：" + clazz.getName());
        }
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName(clazz)).append(" (");
        StringBuilder marks = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
                marks.append(", ");
            }
            sql.append('[').append(columns.get(i).getName()).append(']');
            marks.append('?');
        }
        sql.append(") VALUES (").append(marks).append(')');
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bind(ps, values.toArray());
            return ps.executeUpdate();
        }
    }

    public static <T> int save(T obj) throws Exception {
        try (Connection conn = getConnection()) {
            return save(obj, conn);
        }
    }

    public static <T> int update(T obj, Connection conn) throws Exception {
        if (obj == null) {
            throw new IllegalArgumentException("update 对象不能为空");
        }
        List<Field> fields = getAllFields(obj.getClass());
        Object id = null;
        List<Field> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Field field : fields) {
            Object value = field.get(obj);
            if ("id".equals(field.getName())) {
                id = value;
            } else {
                columns.add(field);
                values.add(value);
            }
        }
        if (id == null) {
            throw new IllegalArgumentException("update 时主键 id 不能为空");
        }
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("实体没有任何可更新的字段");
        }
        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName(obj.getClass())).append(" SET ");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append('[').append(columns.get(i).getName()).append("] = ?");
        }
        sql.append(" WHERE id = ?");
        values.add(id);
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bind(ps, values.toArray());
            return ps.executeUpdate();
        }
    }

    public static <T> int update(T obj) throws Exception {
        try (Connection conn = getConnection()) {
            return update(obj, conn);
        }
    }

    public static <T> int delete(T obj, Connection conn) throws Exception {
        if (obj == null) {
            throw new IllegalArgumentException("delete 对象不能为空");
        }
        Object id = null;
        for (Field field : getAllFields(obj.getClass())) {
            if ("id".equals(field.getName())) {
                id = field.get(obj);
                break;
            }
        }
        if (id == null) {
            throw new IllegalArgumentException("delete 时主键 id 不能为空");
        }
        String sql = "DELETE FROM " + tableName(obj.getClass()) + " WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(id));
            return ps.executeUpdate();
        }
    }

    public static <T> int delete(T obj) throws Exception {
        try (Connection conn = getConnection()) {
            return delete(obj, conn);
        }
    }

    public static int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /* ==================== 参数绑定与类型转换 ==================== */

    public static void bind(PreparedStatement ps, Object... params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            int index = i + 1;
            if (value == null) {
                ps.setObject(index, null);
            } else if (value instanceof LocalDateTime) {
                ps.setTimestamp(index, Timestamp.valueOf((LocalDateTime) value));
            } else if (value instanceof LocalDate) {
                ps.setDate(index, java.sql.Date.valueOf((LocalDate) value));
            } else if (value instanceof Boolean) {
                ps.setString(index, ((Boolean) value) ? "1" : "0");
            } else if (value instanceof Enum) {
                ps.setString(index, ((Enum<?>) value).name());
            } else {
                ps.setObject(index, value);
            }
        }
    }

    private static Object convert(Object value, Class<?> type) {
        if (value == null) {
            return null;
        }
        if (type == String.class) {
            return String.valueOf(value);
        }
        if (type == LocalDateTime.class) {
            if (value instanceof LocalDateTime) {
                return value;
            }
            if (value instanceof Timestamp) {
                return ((Timestamp) value).toLocalDateTime();
            }
            if (value instanceof Date) {
                return LocalDateTime.ofInstant(((Date) value).toInstant(), ZoneId.systemDefault());
            }
            String s = value.toString().trim();
            return LocalDateTime.parse(s.replace(' ', 'T'));
        }
        if (type == LocalDate.class) {
            if (value instanceof LocalDate) {
                return value;
            }
            if (value instanceof java.sql.Date) {
                return ((java.sql.Date) value).toLocalDate();
            }
            if (value instanceof Timestamp) {
                return ((Timestamp) value).toLocalDateTime().toLocalDate();
            }
            if (value instanceof Date) {
                return LocalDateTime.ofInstant(((Date) value).toInstant(), ZoneId.systemDefault()).toLocalDate();
            }
            return LocalDate.parse(value.toString().trim());
        }
        if (type == boolean.class || type == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
            String s = value.toString().trim();
            return "1".equals(s) || "true".equalsIgnoreCase(s) || "是".equals(s);
        }
        if (type == int.class || type == Integer.class) {
            return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString().trim());
        }
        if (type == long.class || type == Long.class) {
            return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString().trim());
        }
        if (type == short.class || type == Short.class) {
            return value instanceof Number ? ((Number) value).shortValue() : Short.parseShort(value.toString().trim());
        }
        if (type == byte.class || type == Byte.class) {
            return value instanceof Number ? ((Number) value).byteValue() : Byte.parseByte(value.toString().trim());
        }
        if (type == double.class || type == Double.class) {
            return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(value.toString().trim());
        }
        if (type == float.class || type == Float.class) {
            return value instanceof Number ? ((Number) value).floatValue() : Float.parseFloat(value.toString().trim());
        }
        if (type == BigDecimal.class) {
            return value instanceof BigDecimal ? value : new BigDecimal(value.toString().trim());
        }
        if (type == java.sql.Date.class && value instanceof Date) {
            return new java.sql.Date(((Date) value).getTime());
        }
        if (type == Timestamp.class && value instanceof Date) {
            return new Timestamp(((Date) value).getTime());
        }
        if (type == Date.class) {
            return value;
        }
        return value;
    }

    /**
     * 便于测试或工具代码输出字段名，不影响正常业务。
     */
    public static Map<String, Object> toMap(Object bean) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Field field : getAllFields(bean.getClass())) {
            map.put(field.getName(), field.get(bean));
        }
        return map;
    }
}