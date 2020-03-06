package com.tony.sqlhelper.helper;

import java.util.Arrays;
import java.util.List;

import com.tony.sqlhelper.EntityManager;
import com.tony.sqlhelper.SchemaHelper;
import com.tony.sqlhelper.domain.PropertyMap;
import com.tony.sqlhelper.proxy.ProxyList;
import com.tony.sqlhelper.query.filter.FilterTuple;
import com.tony.sqlhelper.query.joins.JoinTuple;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RepositoryHelper {

    private static Logger log = LogManager.getLogger(SQLHelper.class);

    public static <T> void fillOneToManyReferences(EntityManager em, T obj, PropertyMap p) {
        try {
            List<?> list = em.GetRepository(p.clazz)
                    .findBy(Arrays.asList(new FilterTuple(p.columnName, SQLObjectHelper.getKeyValue(obj))));
            SQLObjectHelper.setPropertyValue(obj, p.field, list);
            ((ProxyList<?>) list).setTarget(obj, ProxyList.relationOneToMany);
        } catch (IllegalArgumentException | IllegalAccessException e) {
        }
    }

    public static <T> void fillManyToOneReferences(EntityManager em, T obj, PropertyMap p) {
        try {
            log.debug("Filling many to one [" + p.columnName + "] for " + obj);
            Object identifier = SQLObjectHelper.getMapField(obj, p.columnName);
            SQLObjectHelper.setPropertyValue(obj, p.field,
                    em.GetRepository(p.clazz).find(identifier));
        } catch (IllegalArgumentException | IllegalAccessException e) {
        }
    }

    public static <T> void fillOneToOneReferences(EntityManager em, T obj, PropertyMap p) {
        try {
            Object res = null;
            if (!p.columnName.equals("")) {
                Object identifier = SQLObjectHelper.getMapField(obj, p.columnName);
                res = em.GetRepository(p.clazz).find(identifier);
            } else {
                res = em.GetRepository(p.clazz).findOneBy(
                        Arrays.asList(new FilterTuple(p.inversedColumnName, SQLObjectHelper.getKeyValue(obj))));
            }
            SQLObjectHelper.setPropertyValue(obj, p.field, res);
        } catch (IllegalArgumentException | IllegalAccessException e) {
        }
    }

    public static <T> void fillManyToManyReferences(EntityManager em, T obj, PropertyMap p) {
        try {
            String targetPrimaryKey = SchemaHelper.getPrimaryKey(p.clazz).columnName;
            String tableName = SchemaHelper.getTableName(p.clazz);
            List<?> l = em.GetRepository(p.clazz)
                    .Join(new JoinTuple("INNER JOIN", p.tableName,
                            tableName + "." + targetPrimaryKey + " = " + p.tableName + "." + p.inversedColumnName))
                    .findBy(Arrays.asList(new FilterTuple(p.columnName, SQLObjectHelper.getKeyValue(obj))));
            ((ProxyList<?>) l).setTarget(obj, ProxyList.relationManyToMany);
            SQLObjectHelper.setPropertyValue(obj, p.field, l);
        } catch (IllegalArgumentException | IllegalAccessException e) {
        }
    }
}