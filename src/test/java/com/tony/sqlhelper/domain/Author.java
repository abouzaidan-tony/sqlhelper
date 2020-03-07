package com.tony.sqlhelper.domain;

import com.tony.sqlhelper.helper.SQLHelper.SQLTypes;

import java.util.List;

import com.tony.sqlhelper.annotation.ManyToMany;
import com.tony.sqlhelper.annotation.PrimaryKey;
import com.tony.sqlhelper.annotation.Property;
import com.tony.sqlhelper.annotation.Table;

@Table("author")
public class Author {

    @PrimaryKey
    @Property(name="id", type = SQLTypes.Long)
    private Long id;

    @Property(name="name")
    private String name;

    @ManyToMany(targetEntity = Book.class, referencedBy = "author_id", inversedBy = "book_id", joinTable = "author_book")
    private List<Book> books;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Book> getBooks() {
        return books;
    }

    public void setBooks(List<Book> books) {
        this.books = books;
    }
}