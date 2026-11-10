package com.util;

import com.library.entity.Admin;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.BookRecommend;
import com.library.entity.Loan;
import com.library.entity.Operator;
import com.library.entity.Student;
import com.library.entity.Teacher;
import com.library.entity.User;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将实体转换为适合前端使用的 Map，避免直接序列化密码等敏感字段。
 */
public final class Dto {

    private Dto() {
    }

    public static Map<String, Object> user(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (user == null) {
            return map;
        }
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("phone", user.getPhone());
        map.put("role", user.getRole());
        map.put("major", user.getMajor());
        map.put("loginTime", user.getLoginTime());
        map.put("createTime", user.getCreateTime());
        map.put("updateTime", user.getUpdateTime());
        map.put("permissions", user.hasPermission());

        if (user instanceof Admin) {
            Admin admin = (Admin) user;
            map.put("permissionLevel", admin.getPermissionLevel());
            map.put("entryTime", admin.getEntryTime());
        } else if (user instanceof Operator) {
            Operator operator = (Operator) user;
            map.put("area", operator.getArea());
            map.put("duty", operator.getDuty());
        } else if (user instanceof Student) {
            Student student = (Student) user;
            map.put("grade", student.getGrade());
            map.put("college", student.getCollege());
        } else if (user instanceof Teacher) {
            Teacher teacher = (Teacher) user;
            map.put("college", teacher.getCollege());
            map.put("title", teacher.getTitle());
        }
        return map;
    }

    public static Map<String, Object> book(Book book) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (book == null) {
            return map;
        }
        map.put("id", book.getId());
        map.put("title", book.getTitle());
        map.put("author", book.getAuthor());
        map.put("type", book.getType());
        map.put("publisher", book.getPublisher());
        map.put("days", book.getDays());
        map.put("price", book.getPrice());
        map.put("stock", book.getStock());
        map.put("status", book.getStatus());
        map.put("introduction", book.getIntroduction());
        map.put("tags", book.getTags());
        map.put("createTime", book.getCreateTime());
        map.put("updateTime", book.getUpdateTime());
        return map;
    }

    public static Map<String, Object> bookCopy(BookCopy copy) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (copy == null) {
            return map;
        }
        map.put("id", copy.getId());
        map.put("isBorrowed", copy.isBorrowed());
        map.put("createTime", copy.getCreateTime());
        Book book = copy.getBook();
        if (book != null) {
            map.put("bookId", book.getId());
            map.put("bookTitle", book.getTitle());
            map.put("bookAuthor", book.getAuthor());
            map.put("bookType", book.getType());
            map.put("days", book.getDays());
        }
        return map;
    }

    public static Map<String, Object> loan(Loan loan) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (loan == null) {
            return map;
        }
        map.put("id", loan.getId());
        map.put("borrowDate", loan.getBorrowDate());
        map.put("returnDate", loan.getReturnDate());
        map.put("dueDate", loan.getDueDate());
        map.put("status", loan.getStatus());
        map.put("createTime", loan.getCreateTime());

        BookCopy copy = loan.getBookCopy();
        Book book = copy != null ? copy.getBook() : null;
        if (copy != null) {
            map.put("bookCopyId", copy.getId());
            map.put("isBorrowed", copy.isBorrowed());
        }
        if (book != null) {
            map.put("bookId", book.getId());
            map.put("bookTitle", book.getTitle());
            map.put("bookAuthor", book.getAuthor());
            map.put("bookType", book.getType());
            map.put("bookDays", book.getDays());
        }
        User user = loan.getUser();
        if (user != null) {
            map.put("userId", user.getId());
            map.put("userName", user.getName());
            map.put("userRole", user.getRole());
            map.put("userMajor", user.getMajor());
        }

        // 同时保留嵌套结构，和实体设计保持一致，便于前端或后续扩展使用
        if (copy != null) {
            Map<String, Object> copyMap = new LinkedHashMap<>();
            copyMap.put("id", copy.getId());
            copyMap.put("isBorrowed", copy.isBorrowed());
            if (book != null) {
                copyMap.put("book", book(book));
            }
            map.put("bookCopy", copyMap);
        }
        if (user != null) {
            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("id", user.getId());
            userMap.put("name", user.getName());
            userMap.put("role", user.getRole());
            userMap.put("major", user.getMajor());
            map.put("user", userMap);
        }
        return map;
    }

    public static Map<String, Object> recommend(BookRecommend recommend) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (recommend == null) {
            return map;
        }
        map.put("id", recommend.getId());
        map.put("userId", recommend.getUserId());
        map.put("userName", recommend.getUserName());
        map.put("bookName", recommend.getBookName());
        map.put("author", recommend.getAuthor());
        map.put("publisher", recommend.getPublisher());
        map.put("price", recommend.getPrice());
        map.put("status", recommend.getStatus());
        map.put("reason", recommend.getReason());
        map.put("createTime", recommend.getCreateTime());
        map.put("updateTime", recommend.getUpdateTime());
        return map;
    }
}