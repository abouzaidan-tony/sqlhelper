package com.tony.sqlhelper;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.tony.sqlhelper.proxy.ProxyHandler;
import com.tony.sqlhelper.domain.PropertyMap;
import com.tony.sqlhelper.proxy.ProxyList;
import com.tony.sqlhelper.proxy.ProxySQLObject;
import com.tony.sqlhelper.helper.SQLHelper;
import com.tony.sqlhelper.helper.SQLHelper.SQLTypes;
import com.tony.sqlhelper.query.filter.FilterTuple;
import com.tony.sqlhelper.query.joins.JoinTuple;

import javassist.util.proxy.ProxyFactory;

public class Repository<T> {

    private EntityManager em;
    private LinkedList<FilterTuple> filters;
    private LinkedList<JoinTuple> joins;
    private int limit;
    private int offset;
    private Class<T> clazz;
    private ProxyFactory factory;
    private CacheManager cacheManager;


    Repository(EntityManager em, Class<T> clazz, CacheManager cacheManager) {
        this.em = em;
        this.clazz = clazz;
        this.limit = 0;
        this.offset = 0;
        this.filters = new LinkedList<>();
        this.joins = new LinkedList<>();
        this.cacheManager = cacheManager;
        factory = new ProxyFactory();
        factory.setSuperclass(clazz);
        factory.setInterfaces(new Class[] {ProxySQLObject.class});
    }

    public T create() throws Exception {
        return create(null);
    }

    @SuppressWarnings("unchecked")
    private T create(HashMap<String, Object> map) throws Exception {
        ProxyHandler<T> handler = new ProxyHandler<T>(clazz, map, em);
        T instance = (T) factory.create(new Class<?>[0], new Object[0], handler);
        handler.setTarget(instance);
        return instance;
    }

    private String getFetchQuery() {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT ");
        String tableName = SchemaHelper.getTableName(clazz);

        List<PropertyMap> properties = SchemaHelper.getCombinedColumns(clazz);

        for (PropertyMap prop : properties) {
            if(prop.columnName.length() == 0)
                continue;
            builder.append(tableName);
            builder.append('.');
            builder.append(prop.columnName);
            builder.append(',');
            builder.append(' ');
        }
        builder.append("1 ");

        builder.append("FROM ");
        builder.append(tableName);

        for (JoinTuple var : joins) {
            builder.append(' ');
            builder.append(var.toString());
            builder.append(' ');
        }

        if (filters.size() != 0) {
            builder.append(" WHERE ");
            for (FilterTuple var : filters) {
                builder.append(var.toString());
                builder.append(" AND ");
            }

            builder.append("1=1");
        }

        if (limit != 0) {
            builder.append(" limit ");
            builder.append(limit);
        }

        if (offset != 0) {
            builder.append(" offset ");
            builder.append(offset);
        }

        String f_query = builder.toString();
        return f_query;
    }

    public Repository<T> Join(JoinTuple joinTuple) {
        joins.add(joinTuple);
        return this;
    }

    public List<T> findAll() {
        filters.clear();
        return Fetch();
    }

    public List<T> findAll(int limit) {
        filters.clear();
        this.limit = limit;
        List<T> temp = Fetch();
        this.limit = 0;
        return temp;
    }

    public List<T> findAll(int limit, int offset) {
        filters.clear();
        this.limit = limit;
        this.offset = offset;
        List<T> temp = Fetch();
        this.limit = 0;
        this.offset = 0;
        return temp;
    }

    public T findOneBy(List<FilterTuple> filters) {
        limit = 1;
        List<T> list = findBy(filters);
        return list.size() == 0 ? null : list.get(0);
    }

    public T findOneBy(FilterTuple... filters) {
        return findOneBy(Arrays.asList(filters));
    }

    @SuppressWarnings("unchecked")
    public T find(Object primaryKeyValue) {
        if (primaryKeyValue == null)
            return null;
        T temp = (T) cacheManager.fetch(this.clazz, primaryKeyValue);
        if (temp != null)
            return temp;
        PropertyMap key = SchemaHelper.getPrimaryKey(clazz);
        return findOneBy(Arrays.asList(new FilterTuple(key.columnName, primaryKeyValue)));
    }

    public List<T> findBy(List<FilterTuple> filters) {
        this.filters.clear();
        this.filters.addAll(filters);
        return Fetch();
    }

    public final List<T> Fetch() {
        List<HashMap<String, Object>> results = null;
        ProxyList<T> objs = new ProxyList<>();
        int length = filters.size();
        LinkedList<Object> filterParams = new LinkedList<>();

        for (int i = 0; i < length; i++)
            filterParams.add(filters.get(i).Value());
        Object[] filterParamsArray = filterParams.toArray();
        try {
            results = execute(getFetchQuery(), filterParamsArray, SchemaHelper.getColumnsType(clazz));

            for (HashMap<String, Object> entry : results) {
                T instance = create(entry);
                cacheManager.put(clazz, instance);
                objs.disablePropagation();
                objs.add(instance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return objs;
    }

    private List<HashMap<String, Object>> execute(String query, Object[] parameters, SQLTypes[] resultTypes)
            throws ClassNotFoundException {
        
        Connection cnx = EntityManager.GetInstance().getConnection();
        if(cnx == null)
            return SQLHelper.GetInstance().ExecuteQuery(query, parameters, resultTypes);
        else
            return SQLHelper.GetInstance().ExecuteQuery(cnx, query, parameters, resultTypes);
    }
}