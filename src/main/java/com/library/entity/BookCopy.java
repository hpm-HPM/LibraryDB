package com.library.entity;

/**
 * 图书副本。isBorrowed 对应数据库 varchar(1) 的 '1'/'0'。
 */
public class BookCopy extends Base {

    private static final long serialVersionUID = 1L;

    private Book book;
    private boolean isBorrowed;

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public boolean isBorrowed() {
        return isBorrowed;
    }

    public void setBorrowed(boolean borrowed) {
        isBorrowed = borrowed;
    }
}