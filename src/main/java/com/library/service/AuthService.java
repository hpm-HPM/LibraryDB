package com.library.service;

import com.library.dao.UserDao;
import com.library.entity.Admin;
import com.library.entity.Operator;
import com.library.entity.Student;
import com.library.entity.Teacher;
import com.library.entity.User;
import com.util.JDBCTool;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 登录、注册等认证业务。
 */
public class AuthService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final List<String> ROLES = Arrays.asList("管理员", "操作员", "学生", "教师");

    private final UserDao userDao = new UserDao();

    public User login(String name, String password, String captcha, String expectedCaptcha) throws Exception {
        if (isBlank(captcha)) {
            throw new BusinessException("请输入验证码");
        }
        if (isBlank(expectedCaptcha)) {
            throw new BusinessException("验证码已过期，请刷新后重试");
        }
        if (!captcha.trim().equalsIgnoreCase(expectedCaptcha)) {
            throw new BusinessException("验证码错误");
        }
        String loginName = ServiceSupport.requireText(name, "用户名");
        if (isBlank(password)) {
            throw new BusinessException("密码不能为空");
        }
        try (Connection conn = JDBCTool.getConnection()) {
            User user = userDao.findByName(conn, loginName);
            if (user == null || !password.equals(user.getPassword())) {
                throw new BusinessException("用户名或密码错误");
            }
            LocalDateTime now = LocalDateTime.now();
            userDao.updateLoginTime(conn, user.getId(), now);
            user.setLoginTime(now);
            return user;
        }
    }

    /**
     * 注册。前端根据角色提交不同的扩展字段。
     */
    public User register(Map<String, String> form) throws Exception {
        String name = ServiceSupport.requireText(form.get("name"), "姓名");
        if (name.length() > 30) {
            throw new BusinessException("姓名长度不能超过 30 个字符");
        }
        String password = form.get("password");
        if (isBlank(password) || password.length() < 6 || password.length() > 20) {
            throw new BusinessException("密码长度应为 6-20 位");
        }
        String phone = ServiceSupport.requireText(form.get("phone"), "手机号");
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException("手机号格式不正确");
        }
        String role = normalizeRole(ServiceSupport.requireText(form.get("role"), "角色"));
        if (!ROLES.contains(role)) {
            throw new BusinessException("角色不合法");
        }
        String major = ServiceSupport.requireText(form.get("major"), "专业");

        User user;
        if ("管理员".equals(role)) {
            Admin admin = new Admin();
            admin.setPermissionLevel(ServiceSupport.parseInt(form.get("permissionLevel"), "权限等级", 1));
            admin.setEntryTime(parseDate(form.get("entryTime"), LocalDate.now(), "入职日期"));
            user = admin;
        } else if ("操作员".equals(role)) {
            Operator operator = new Operator();
            operator.setArea(ServiceSupport.requireText(form.get("area"), "工作区域"));
            operator.setDuty(ServiceSupport.requireText(form.get("duty"), "工作职责"));
            user = operator;
        } else if ("教师".equals(role)) {
            Teacher teacher = new Teacher();
            teacher.setCollege(ServiceSupport.requireText(form.get("college"), "所属学院"));
            teacher.setTitle(ServiceSupport.requireText(form.get("title"), "职称"));
            user = teacher;
        } else {
            Student student = new Student();
            student.setGrade(ServiceSupport.requireText(form.get("grade"), "年级"));
            student.setCollege(ServiceSupport.requireText(form.get("college"), "所属学院"));
            user = student;
        }

        ServiceSupport.initBase(user);
        user.setName(name);
        user.setPassword(password);
        user.setPhone(phone);
        user.setRole(role);
        user.setMajor(major);

        try (Connection conn = JDBCTool.getConnection()) {
            if (userDao.existsName(conn, name)) {
                throw new BusinessException("该姓名已被注册");
            }
            if (userDao.existsPhone(conn, phone)) {
                throw new BusinessException("该手机号已被注册");
            }
            userDao.insert(conn, user);
        }
        return user;
    }

    private String normalizeRole(String role) {
        switch (role.trim().toLowerCase()) {
            case "admin":
                return "管理员";
            case "operator":
                return "操作员";
            case "student":
                return "学生";
            case "teacher":
                return "教师";
            default:
                return role.trim();
        }
    }

    private LocalDate parseDate(String value, LocalDate defaultValue, String label) {
        if (isBlank(value)) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException(label + "格式应为 yyyy-MM-dd");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}