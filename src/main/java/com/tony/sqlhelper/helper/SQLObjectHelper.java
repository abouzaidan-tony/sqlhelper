package com.tony.sqlhelper.helper;

import java.util.List;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigInteger;

import com.tony.sqlhelper.SchemaHelper;
import com.tony.sqlhelper.annotation.ManyToOne;
import com.tony.sqlhelper.annotation.OneToOne;
import com.tony.sqlhelper.annotation.Table;
import com.tony.sqlhelper.domain.PropertyMap;
import com.tony.sqlhelper.proxy.ProxyHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javassist.util.proxy.ProxyObject;

public abstract class SQLObjectHelper {

    private static Logger log = LogManager.getLogger(SQLObjectHelper.class);

    public static final Object getPropertyValue(Object obj, Field field) {
        field.setAccessible(true);
        try {
            return field.get(obj);
        } catch (IllegalAccessException | IllegalArgumentException e) {
        }
        return null;
    }

    public static final void setPropertyValue(Object obj, Field field, Object value)
            throws IllegalArgumentException, IllegalAccessException {
        field.setAccessible(true);
        if (value instanceof BigInteger && field.getType().equals(Long.class))
            field.set(obj, ((BigInteger) value).longValue());
        else
            field.set(obj, value);
    }

    public static final <T> Object[] getPropertyArray(Object obj, List<PropertyMap> properties) {

        log.trace("Getting properties array");

        Object[] pros = new Object[properties.size()];

        int i = 0;
        for (PropertyMap var : properties) {
            Object res = getPropertyValue(obj, var.field);
            if (isEntity(var.field.getType()))
                pros[i++] = getOriginalKeyValue(var.field, obj, res);
            else
                pros[i++] = res;
        }

        return pros;
    }

    public static final Class<?> getEntityClass(Class<?> clazz) {
        while (clazz != null) {
            for (Annotation a : clazz.getAnnotations())
                if (a instanceof Table)
                    return clazz;
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    public static final boolean isEntity(Class<?> clazz) {
        return getEntityClass(clazz) != null;
    }

    public static final Object getOriginalKeyValue(Field f, Object obj, Object target) {
        if (target != null)
            return getKeyValue(target);

        log.trace("Original obj is null for " + f.getName());
        for (Annotation a : f.getAnnotations()) {
            if (a instanceof OneToOne)
                return getMapField(obj, ((OneToOne) a).inverserdBy());
            if (a instanceof ManyToOne)
                return getMapField(obj, ((ManyToOne) a).inverserdBy());
        }

        return null;

    }

    public static final Object getKeyValue(Object obj) {
        if (obj == null)
            return null;
        Field field = SchemaHelper.getPrimaryKey(obj.getClass()).field;
        return getPropertyValue(obj, field);
    }

    public static final <T> Object getMapField(Object obj, String columnName) {
        ProxyHandler<T> handler = getHandler(obj);
        if (handler == null)
            return null;
        if (handler.getMap() == null)
            return null;
        return handler.getMap().get(columnName);
    }

    public static final <T> void setMapField(Object obj, String columnName, Object val) {
        ProxyHandler<T> handler = getHandler(obj);
        if (handler == null)
            return;
        handler.initMap();
        handler.getMap().put(columnName, val);
    }

    @SuppressWarnings("unchecked")
    public static final <T> ProxyHandler<T> getHandler(Object obj) {
        if(obj instanceof ProxyObject)
            return (ProxyHandler<T>) ((ProxyObject) obj).getHandler();
        return null;
    }
}