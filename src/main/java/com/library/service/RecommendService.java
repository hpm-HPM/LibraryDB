package com.library.service;

import com.library.dao.BookCopyDao;
import com.library.dao.BookDao;
import com.library.dao.BookRecommendDao;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.BookRecommend;
import com.library.entity.User;
import com.util.Dto;
import com.util.JDBCTool;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师推荐图书与管理员审阅。
 */
public class RecommendService {

    private final BookDao bookDao = new BookDao();
    private final BookCopyDao copyDao = new BookCopyDao();
    private final BookRecommendDao recommendDao = new BookRecommendDao();

    /**
     * 推荐前检查馆藏：存在匹配图书时返回图书信息，否则创建一条待审核推荐。
     * 返回 {exists:boolean, books:[...], recommend:{...}, message}
     */
    public Map<String, Object> check(User user, Map<String, String> form) throws Exception {
        String bookName = ServiceSupport.maxLength(
                ServiceSupport.requireText(form.get("bookName"), "书名"), 20, "书名");
        String author = ServiceSupport.maxLength(
                ServiceSupport.requireText(form.get("author"), "作者"), 20, "作者");
        String publisher = ServiceSupport.maxLength(form.get("publisher"), 20, "出版社");
        double price = ServiceSupport.parseDouble(form.get("price"), "价格", 0);
        if (price < 0) {
            throw new BusinessException("价格不能为负数");
        }

        try (Connection conn = JDBCTool.getConnection()) {
            List<Book> matches = bookDao.findByTitleLike(conn, bookName);
            Map<String, Object> result = new LinkedHashMap<>();
            if (!matches.isEmpty()) {
                List<Map<String, Object>> books = new ArrayList<>();
                for (Book book : matches) {
                    books.add(Dto.book(book));
                }
                result.put("exists", true);
                result.put("books", books);
                result.put("recommend", null);
                result.put("message", "馆藏中已找到相关图书，请先确认是否已存在，无需重复推荐");
                return result;
            }

            BookRecommend recommend = new BookRecommend();
            ServiceSupport.initBase(recommend);
            recommend.setUserId(user.getId());
            recommend.setBookName(bookName);
            recommend.setAuthor(author);
            recommend.setPublisher(publisher);
            recommend.setPrice(price);
            recommend.setStatus("待审核");
            recommend.setReason(null);
            recommendDao.insert(conn, recommend);
            recommend.setUserName(user.getName());

            result.put("exists", false);
            result.put("books", new ArrayList<>());
            result.put("recommend", Dto.recommend(recommend));
            result.put("message", "馆藏中没有找到该图书，推荐已提交，等待管理员审核");
            return result;
        }
    }

    public List<BookRecommend> mine(User user) throws Exception {
        try (Connection conn = JDBCTool.getConnection()) {
            return recommendDao.findByUser(conn, user.getId());
        }
    }

    public List<BookRecommend> pending() throws Exception {
        try (Connection conn = JDBCTool.getConnection()) {
            return recommendDao.findPending(conn);
        }
    }

    /**
     * 审阅推荐：approve=true 通过并自动入库、生成副本；否则驳回并记录理由。
     */
    public void review(User reviewer, Map<String, String> form) throws Exception {
        String id = ServiceSupport.requireText(form.get("id"), "推荐记录ID");
        boolean approve = "true".equalsIgnoreCase(form.get("approve")) || "1".equals(form.get("approve"));
        String reason = ServiceSupport.maxLength(form.get("reason"), 100, "驳回理由");

        Tx.execute(conn -> {
            BookRecommend recommend = recommendDao.findById(conn, id);
            if (recommend == null) {
                throw new BusinessException("推荐记录不存在");
            }
            if (!"待审核".equals(recommend.getStatus())) {
                throw new BusinessException("该推荐已审阅，不能重复操作");
            }
            if (!approve) {
                if (reason == null || reason.isEmpty()) {
                    throw new BusinessException("驳回时必须填写理由");
                }
                recommendDao.updateReview(conn, id, "已驳回", reason);
                return null;
            }

            String type = ServiceSupport.maxLength(form.get("type"), 10, "图书类型");
            if (type == null || type.isEmpty()) {
                type = "综合";
            }
            int days = ServiceSupport.parseInt(form.get("days"), "可借天数", 7);
            int stock = ServiceSupport.parseInt(form.get("stock"), "库存", 1);
            if (days <= 0) {
                throw new BusinessException("可借天数必须大于 0");
            }
            if (stock < 0 || stock > 1000) {
                throw new BusinessException("库存数量应在 0-1000 之间");
            }

            Book book = new Book();
            ServiceSupport.initBase(book);
            book.setTitle(recommend.getBookName());
            book.setAuthor(recommend.getAuthor());
            book.setType(type);
            book.setPublisher(recommend.getPublisher() == null ? "" : recommend.getPublisher());
            book.setDays(days);
            book.setPrice(recommend.getPrice() == null ? 0 : recommend.getPrice());
            book.setStock(stock);
            book.setStatus("正常");
            String recommender = recommend.getUserName() == null ? "教师" : recommend.getUserName();
            book.setIntroduction("由 " + recommender + " 推荐，审核通过后入库。");
            book.setTags("推荐图书");
            bookDao.insert(conn, book);

            for (int i = 0; i < stock; i++) {
                BookCopy copy = new BookCopy();
                ServiceSupport.initBase(copy);
                copy.setBorrowed(false);
                copyDao.insert(conn, copy, book.getId());
            }
            recommendDao.updateReview(conn, id, "已通过", reason);
            return null;
        });
    }
}