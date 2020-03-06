package com.tony.sqlhelper.domain;

import java.sql.SQLException;

@FunctionalInterface
public interface SQLExceptionConsumer<T>{

    public void accept(T object) throws SQLException;
}