package com.tony.sqlhelper.domain;

import java.lang.reflect.Field;

import com.tony.sqlhelper.helper.SQLHelper.SQLTypes;

public class PropertyMap {
    public final Field field;
    public final String columnName;
    public final String inversedColumnName;
    public final SQLTypes type;
    public final boolean isPrimary;
    public final String tableName;
    public final Class<?> clazz;

    public PropertyMap(Field field, String columnName, SQLTypes type, boolean isPrimary) {
        this.field = field;
        this.columnName = columnName;
        this.type = type;
        this.isPrimary = isPrimary;
        clazz = null;
        inversedColumnName = null;
        tableName = null;
    }

    public PropertyMap(Field field, String columnName, Class<?> clazz) {
        this.field = field;
        this.columnName = columnName;
        type = null;
        isPrimary = false;
        this.clazz = clazz;
        inversedColumnName = null;
        tableName = null;
    }

    public PropertyMap(Field field, String columnName, String inversedColumnName, Class<?> clazz,
            SQLTypes type) {
        this.field = field;
        this.columnName = columnName;
        this.type = type;
        isPrimary = false;
        this.clazz = clazz;
        this.inversedColumnName = inversedColumnName;
        tableName = null;
    }

    public PropertyMap(Field field, String columnName, Class<?> clazz, SQLTypes type) {
        this.field = field;
        this.columnName = columnName;
        this.type = type;
        isPrimary = false;
        this.clazz = clazz;
        inversedColumnName = null;
        tableName = null;
    }

    public PropertyMap(Field field, String columnName, String inversedColumnName, String tableName,
            Class<?> clazz, SQLTypes type) {
        this.field = field;
        this.columnName = columnName;
        this.type = type;
        isPrimary = false;
        this.clazz = clazz;
        this.inversedColumnName = inversedColumnName;
        this.tableName = tableName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PropertyMap))
            return false;
        PropertyMap map = (PropertyMap) obj;
        return this.columnName.equals(map.columnName);
    }
}