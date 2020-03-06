package com.tony.sqlhelper;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.tony.sqlhelper.domain.PropertyMap;
import com.tony.sqlhelper.domain.SQLExceptionConsumer;
import com.tony.sqlhelper.helper.SQLHelper;
import com.tony.sqlhelper.helper.SQLObjectHelper;
import com.tony.sqlhelper.proxy.ProxySQLObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EntityManager {
    

    private static final String savePointPrefix = "SAVEPOINT_";
   
    

    private static Logger log = LogManager.getLogger(EntityManager.class);

    private static EntityManager instance;

    private static HashMap<Class<?>, List<PropertyMap>> insertProperties;
    private static HashMap<Class<?>, List<PropertyMap>> updateProperties;
    private static HashMap<Class<?>, String> insertQueries;
    private static HashMap<Class<?>, String> updateQueries;
    private static HashMap<Class<?>, String> deleteQueries;

    

    static {
        insertQueries = new HashMap<>();
        updateQueries = new HashMap<>();
        deleteQueries = new HashMap<>();
        insertProperties = new HashMap<>();
        updateProperties = new HashMap<>();
    }

    private HashMap<String, Integer> savepointNameToCounter;
    private HashMap<Integer, String> counterToSavepoint;
    private CacheManager cacheManager;
    private int savePointCounter;
    private SQLHelper helper;
    private Connection connection;
    private long lastR;

    public EntityManager() {
        try {
            helper = SQLHelper.GetInstance();
        } catch (ClassNotFoundException e) {
            log.error("Cannot find Driver class", e);
        }
        cacheManager = new CacheManager();
        cacheManager.setState(savePointCounter);
        savepointNameToCounter = new HashMap<>();
        savePointCounter = 0;
        counterToSavepoint = new HashMap<>();
        lastR = -1;
    }

    private long getRand(){
        return ++lastR;
    }

    public <C> Repository<C> GetRepository(Class<C> clazz) {
        return new Repository<>(this, clazz, cacheManager);
    }

    public static EntityManager GetInstance() {
        if (instance == null)
            instance = new EntityManager();
        if(instance.helper == null)
            throw new IllegalStateException("SQL Helper not initialized");
        return instance;
    }

    public void beginTransaction() throws SQLException {
        if (connection != null) {
            connection.close();
        }
        connection = helper.getConnection();
        connection.setAutoCommit(false);
        cacheManager.setTransactionMode(true);
    }

    Connection getConnection(){
        return connection;
    }

    public void commit() throws SQLException {
        if (connection == null)
            throw new IllegalStateException("Connection is null");
        cacheManager.commit();
        connection.commit();
        connection.close();
        savepointNameToCounter.clear();
        connection = null;
        savePointCounter = 0;
        
    }

    public Savepoint setSavePoint() throws SQLException {
        if (connection == null)
            throw new IllegalStateException("Connection is null");
        Savepoint sp = connection.setSavepoint(savePointPrefix + Integer.toString(++savePointCounter));
        cacheManager.setState(savePointCounter);
        savepointNameToCounter.put(sp.getSavepointName(), savePointCounter);
        return sp;
    }

    public void rollBack() throws SQLException {
        if (connection == null)
            throw new IllegalStateException("Connection is null");
        connection.rollback();
        connection.close();
        cacheManager.rolllback();
        savepointNameToCounter.clear();
        savePointCounter = 0;
        connection = null;
    }

    public void rollBack(Savepoint savepoint) throws SQLException {
        if (connection == null)
            throw new IllegalStateException("Connection is null");

        Integer oldCounter = savepointNameToCounter.get(savepoint.getSavepointName());
        if(oldCounter == null)
            throw new IllegalArgumentException("Savepoint not valid [Future Savepoint]");

        clearSavepointsOnRollback(oldCounter);
        connection.rollback(savepoint);
        cacheManager.rolllback(savePointCounter);
    }

    private void clearSavepointsOnRollback(int oldCounter){
        String savePointName;
        for(int i=savePointCounter; i>oldCounter; i--){
            savePointName = counterToSavepoint.get(i);
            savepointNameToCounter.remove(savePointName);
            counterToSavepoint.remove(i);
        }
        savePointCounter = oldCounter;
    }

    private String getTableName(Class<?> clazz) {
        return SchemaHelper.getTableName(clazz);
    }

    private String getMainUpdateQuery(Class<?> clazz) {
        return "UPDATE " + getTableName(clazz) + " SET @vals WHERE @cond";
    }

    private final String getMainDeleteQuery(Class<?> clazz) {
        return "DELETE FROM " + getTableName(clazz) + " WHERE @cond";
    }

    private final String getMainInsertQuery(Class<?> clazz) {
        return "INSERT INTO " + getTableName(clazz) + "  (@columnsName) VALUES (@vals)";
    }

    private synchronized String getInsertQuery(Class<?> clazz) {
        String query = insertQueries.get(clazz);
        if (query != null)
            return query;

        StringBuilder builder1 = new StringBuilder();
        StringBuilder builder2 = new StringBuilder();

        boolean first = true;

        List<PropertyMap> properties = SchemaHelper.getCombinedColumns(clazz);

        List<PropertyMap> usedProperties = new ArrayList<>();

        for (PropertyMap prop : properties) {

            if (prop.columnName.length() == 0)
                continue;
            if (first)
                first = false;
            else {
                builder1.append(',');
                builder2.append(',');
            }

            usedProperties.add(prop);
            builder1.append(prop.columnName);
            builder2.append('?');
        }

        query = getMainInsertQuery(clazz).replace("@columnsName", builder1.toString()).replace("@vals",
                builder2.toString());
        insertProperties.put(clazz, usedProperties);
        insertQueries.put(clazz, query);
        return query;
    }

    private synchronized String getUpdateQuery(Class<?> clazz){
        String query = updateQueries.get(clazz);
        if (query != null)
            return query;

        StringBuilder builder = new StringBuilder();

        boolean first = true;

        List<PropertyMap> properties = SchemaHelper.getCombinedColumns(clazz);

        List<PropertyMap> usedProperties = new ArrayList<>();

        for (PropertyMap prop : properties) {

            if (prop.isPrimary)
                continue;

            if (prop.columnName.length() == 0)
                continue;

            if (first)
                first = false;
            else
                builder.append(", ");

            usedProperties.add(prop);
            builder.append(prop.columnName);
            builder.append("= ?");
        }

        PropertyMap key = SchemaHelper.getPrimaryKey(clazz);
        usedProperties.add(key);

        query = getMainUpdateQuery(clazz).replace("@vals", builder.toString()).replace("@cond",
                key.columnName + "= ?");
        updateQueries.put(clazz, query);
        updateProperties.put(clazz, usedProperties);
        return query;
    }

    private synchronized String getDeleteQuery(Class<?> clazz){
        String query = deleteQueries.get(clazz);
        if (query != null)
            return query;

        PropertyMap key = SchemaHelper.getPrimaryKey(clazz);
        query = getMainDeleteQuery(clazz).replace("@cond", key.columnName + "= ?");
        deleteQueries.put(clazz, query);
        return query;
    }


    @SuppressWarnings("unchecked")
    private void executeToRelatedObjects(Object obj, SQLExceptionConsumer<Object> consumer) throws SQLException {
        List<PropertyMap> ps = new ArrayList<>(SchemaHelper.getOneToOneColumns((Class<?>) obj.getClass()));
        for (PropertyMap p : ps) {
            p.field.setAccessible(true);
            try {
                consumer.accept(p.field.get(obj));
            } catch (IllegalArgumentException | IllegalAccessException e) {
                log.error("Cannot set Primary key for class [" + obj.getClass() + "]", e);
            }
        }
        ps = SchemaHelper.getOneToManyColumns((Class<?>) obj.getClass());
        for (PropertyMap p : ps) {
            p.field.setAccessible(true);
            try {
                Object o = p.field.get(obj);
                if(o != null){
                    for (Object e : (List<Object>) o)
                            consumer.accept(e);
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                log.error("Cannot set Primary key for class [" + obj.getClass() + "]", e);
            }
        }
    }

    private void setAccessNumber(Object obj, long n)
    {
        ((ProxySQLObject)obj).setAccessNumber(n);
    }

    private boolean validateAccessNumber(Object obj, long n) {
        if (obj instanceof ProxySQLObject) {
            return ((ProxySQLObject) obj).getAccessNumber() != n;
        }
        throw new IllegalArgumentException("Object not intiantiated from EntityManager");
    }

    public void persist(Object object) throws SQLException {
        persist(object, getRand());
    }

    private void persist(Object object, long accessNumber) throws SQLException {
        if (object == null) {
            log.warn("insert null obj");
            return;
        }
        if(!validateAccessNumber(object, accessNumber))
            return;
        setAccessNumber(object, accessNumber);

        Class<?> clazz = object.getClass();

        if(object instanceof ProxySQLObject)
            clazz = ((ProxySQLObject)object).getEntityClass();

        Object key = SQLObjectHelper.getMapField(object, SchemaHelper.getPrimaryKey(clazz).columnName);
        if (key == null)
            insert(object);
        else
            update(object);
        cacheManager.put(clazz, object);
        executeToRelatedObjects(object, s -> persist(s, accessNumber));
    }

    public void remove(Object object) throws SQLException {
       delete(object, getRand());
    }

    public void delete(Object object, long accessNumber) throws SQLException {
        if (object == null) {
            log.warn("update null obj");
            return;
        }
        if (!validateAccessNumber(object, accessNumber))
            return;
        setAccessNumber(object, accessNumber);

        executeToRelatedObjects(object, s -> delete(s, accessNumber));
        delete(object);
    }

    private Object execute(String query, Object[] args) throws SQLException {
        if (connection == null)
            return helper.ExecuteNonQuery(query, args);
        else
            return helper.ExecuteNonQuery(connection, query, args);
    }

    private void insert(Object object) throws SQLException {

        String query = getInsertQuery(object.getClass());

        Object key = execute(query, SQLObjectHelper.getPropertyArray(object, insertProperties.get(object.getClass())));

        SQLObjectHelper.setMapField(object, SchemaHelper.getPrimaryKey(object.getClass()).columnName, key);

        try {
            SQLObjectHelper.setPropertyValue(object, SchemaHelper.getPrimaryKey(object.getClass()).field, key);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            log.error("Cannot set Primary key for class [" + object.getClass() + "]", e);
        }
    }

    private void update(Object object) throws SQLException {
        execute(getUpdateQuery(object.getClass()), 
            SQLObjectHelper.getPropertyArray(object, updateProperties.get(object.getClass())));
    }

    private void delete(Object object) throws SQLException {
        Class<?> clazz = object.getClass();
        Object id = SQLObjectHelper.getKeyValue(object);
        Object[] params = new Object[] { id };
        execute(getDeleteQuery(object.getClass()), params);
        try {
            SQLObjectHelper.setPropertyValue(object, SchemaHelper.getPrimaryKey(clazz).field, null);
        } catch (Exception e) {
            try {
                SQLObjectHelper.setPropertyValue(object, SchemaHelper.getPrimaryKey(clazz).field, 0);
            } catch (Exception ex) {
            }
        }
    }
}