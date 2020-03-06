package com.tony.sqlhelper.query.filter;

public class LessFilterTuple extends FilterTuple {

    public LessFilterTuple(String columnName, Object value) {
        super(columnName, value);
    }

    @Override
    public String toString() {
        return columnName + " < " + " ? ";
    }
}