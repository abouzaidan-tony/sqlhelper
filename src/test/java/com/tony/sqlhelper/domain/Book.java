package com.tony.sqlhelper.domain;

import com.tony.sqlhelper.helper.SQLHelper.SQLTypes;

import java.util.List;

import com.tony.sqlhelper.annotation.ManyToMany;
import com.tony.sqlhelper.annotation.PrimaryKey;
import com.tony.sqlhelper.annotation.Property;
import com.tony.sqlhelper.annotation.Table;

@Table("book")
public class Book {

    @PrimaryKey
    @Property(name="id", type = SQLTypes.Long)
    private Long id;

    @Property(name="name")
    private String name;

    @ManyToMany(targetEntity = Author.class, mappedBy = "author_id", inversedBy = "book_id", joinTable = "author_book")
    private List<Author> authors;

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

    public List<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(List<Author> authors) {
        this.authors = authors;
    }
}