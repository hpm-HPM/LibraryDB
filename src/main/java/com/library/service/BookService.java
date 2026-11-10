package com.library.service;

import com.library.dao.BookCopyDao;
import com.library.dao.BookDao;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.util.JDBCTool;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * 图书与副本相关的业务规则。
 */
public class BookService {

    private final BookDao bookDao = new BookDao();
    private final BookCopyDao copyDao = new BookCopyDao();

    public List<Book> search(String keyword) throws Exception {
        try (Connection conn = JDBCTool.getConnection()) {
            return bookDao.search(conn, keyword);
        }
    }

    public List<Book> listBorrowable() throws Exception {
        try (Connection conn = JDBCTool.getConnection()) {
            return bookDao.listBorrowable(conn);
        }
    }

    public List<String> listTypes() throws Exception {
        try (Connection conn = JDBCTool.getConnection()) {
            return bookDao.listTypes(conn);
        }
    }

    public List<BookCopy> listCopies(String bookId) throws Exception {
        try (Connection conn = JDBCTool.getConnection()) {
            return copyDao.findByBook(conn, bookId);
        }
    }

    public Book detail(String id) throws Exception {
        try (Connection conn = JDBCTool.getConnection()) {
            return bookDao.findById(conn, id);
        }
    }

    public Book add(Map<String, String> form) throws Exception {
        String title = ServiceSupport.maxLength(ServiceSupport.requireText(form.get("title"), "书名"), 30, "书名");
        String author = ServiceSupport.maxLength(ServiceSupport.requireText(form.get("author"), "作者"), 30, "作者");
        String type = ServiceSupport.maxLength(ServiceSupport.requireText(form.get("type"), "类型"), 10, "类型");
        String publisher = ServiceSupport.maxLength(form.get("publisher"), 20, "出版社");
        String introduction = ServiceSupport.maxLength(form.get("introduction"), 512, "简介");
        String tags = ServiceSupport.maxLength(form.get("tags"), 500, "标签");
        int days = ServiceSupport.parseInt(form.get("days"), "可借天数", 7);
        double price = ServiceSupport.parseDouble(form.get("price"), "价格", 0);
        int stock = ServiceSupport.parseInt(form.get("stock"), "库存", 1);
        if (days <= 0) {
            throw new BusinessException("可借天数必须大于 0");
        }
        if (price < 0) {
            throw new BusinessException("价格不能为负数");
        }
        if (stock < 0 || stock > 1000) {
            throw new BusinessException("库存数量应在 0-1000 之间");
        }

        Book book = new Book();
        ServiceSupport.initBase(book);
        book.setTitle(title);
        book.setAuthor(author);
        book.setType(type);
        book.setPublisher(publisher);
        book.setDays(days);
        book.setPrice(price);
        book.setStock(stock);
        book.setStatus("正常");
        book.setIntroduction(introduction);
        book.setTags(tags);

        Tx.execute(conn -> {
            bookDao.insert(conn, book);
            for (int i = 0; i < stock; i++) {
                BookCopy copy = new BookCopy();
                ServiceSupport.initBase(copy);
                copy.setBorrowed(false);
                copyDao.insert(conn, copy, book.getId());
            }
            return null;
        });
        return book;
    }

    /**
     * 编辑图书：只更新展示信息，不修改库存、状态与副本。
     */
    public void update(Map<String, String> form) throws Exception {
        String id = ServiceSupport.requireText(form.get("id"), "图书ID");
        Book existing = detail(id);
        if (existing == null) {
            throw new BusinessException("图书不存在");
        }
        if ("已废弃".equals(existing.getStatus())) {
            throw new BusinessException("已废弃的图书不能编辑");
        }
        String title = ServiceSupport.maxLength(ServiceSupport.requireText(form.get("title"), "书名"), 30, "书名");
        String author = ServiceSupport.maxLength(ServiceSupport.requireText(form.get("author"), "作者"), 30, "作者");
        String type = ServiceSupport.maxLength(ServiceSupport.requireText(form.get("type"), "类型"), 10, "类型");
        String publisher = form.containsKey("publisher")
                ? ServiceSupport.maxLength(form.get("publisher"), 20, "出版社") : existing.getPublisher();
        int days = ServiceSupport.parseInt(form.get("days"), "可借天数", existing.getDays() == null ? 7 : existing.getDays());
        double price = ServiceSupport.parseDouble(form.get("price"), "价格",
                existing.getPrice() == null ? 0 : existing.getPrice());
        if (days <= 0) {
            throw new BusinessException("可借天数必须大于 0");
        }
        if (price < 0) {
            throw new BusinessException("价格不能为负数");
        }

        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setType(type);
        book.setPublisher(publisher);
        book.setDays(days);
        book.setPrice(price);
        // 按作业要求，编辑只更新上述六项展示字段，不修改简介、标签、库存、状态与副本。

        try (Connection conn = JDBCTool.getConnection()) {
            bookDao.updateEditable(conn, book);
        }
    }

    /**
     * 废弃图书：必须所有副本已归还，且可借库存等于副本总数。
     */
    public void discard(String bookId) throws Exception {
        String id = ServiceSupport.requireText(bookId, "图书ID");
        Tx.execute(conn -> {
            Book book = bookDao.findByIdForUpdate(conn, id);
            if (book == null) {
                throw new BusinessException("图书不存在");
            }
            if ("已废弃".equals(book.getStatus())) {
                throw new BusinessException("该图书已经是废弃状态");
            }
            int borrowed = bookDao.countBorrowedCopies(conn, id);
            if (borrowed > 0) {
                throw new BusinessException("该图书还有 " + borrowed + " 个副本未归还，不能废弃");
            }
            int copies = bookDao.countCopies(conn, id);
            int stock = book.getStock() == null ? 0 : book.getStock();
            if (stock != copies) {
                throw new BusinessException("可借副本数（" + stock + "）与副本总数（" + copies + "）不一致，不能废弃");
            }
            bookDao.discard(conn, id);
            return null;
        });
    }

    /**
     * 新增副本，库存 +1。
     */
    public BookCopy addCopy(String bookId) throws Exception {
        String id = ServiceSupport.requireText(bookId, "图书ID");
        return Tx.execute(conn -> {
            Book book = bookDao.findByIdForUpdate(conn, id);
            if (book == null) {
                throw new BusinessException("图书不存在");
            }
            if ("已废弃".equals(book.getStatus())) {
                throw new BusinessException("已废弃的图书不能新增副本");
            }
            BookCopy copy = new BookCopy();
            ServiceSupport.initBase(copy);
            copy.setBorrowed(false);
            copyDao.insert(conn, copy, id);
            if (bookDao.adjustStock(conn, id, 1) == 0) {
                throw new BusinessException("新增副本失败，库存更新异常");
            }
            copy.setBook(book);
            return copy;
        });
    }

    /**
     * 删除副本，库存 -1。借阅中的副本、存在借阅历史的副本不能删除。
     */
    public void deleteCopy(String copyId) throws Exception {
        String id = ServiceSupport.requireText(copyId, "副本ID");
        Tx.execute(conn -> {
            BookCopy copy = copyDao.findById(conn, id);
            if (copy == null) {
                throw new BusinessException("副本不存在");
            }
            if (copy.isBorrowed()) {
                throw new BusinessException("该副本正在借阅中，不能删除");
            }
            Book book = bookDao.findByIdForUpdate(conn, copy.getBook().getId());
            if (book == null) {
                throw new BusinessException("副本对应的图书不存在");
            }
            if ("已废弃".equals(book.getStatus())) {
                throw new BusinessException("已废弃的图书不能删除副本");
            }
            int history = copyDao.countLoans(conn, id);
            if (history > 0) {
                throw new BusinessException("该副本存在借阅记录，为保证借阅数据完整不能删除");
            }
            if (book.getStock() == null || book.getStock() <= 0) {
                throw new BusinessException("库存数据异常，不能删除副本");
            }
            copyDao.delete(conn, id);
            bookDao.adjustStock(conn, book.getId(), -1);
            return null;
        });
    }

    public int countCopies(String bookId) throws Exception {
        try (Connection conn = JDBCTool.getConnection()) {
            return copyDao.countByBook(conn, bookId);
        }
    }

    public int countBorrowedCopies(String bookId) throws Exception {
        try (Connection conn = JDBCTool.getConnection()) {
            return copyDao.countBorrowedByBook(conn, bookId);
        }
    }
}