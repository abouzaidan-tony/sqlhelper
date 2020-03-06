package com.tony.sqlhelper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import com.tony.sqlhelper.helper.SQLObjectHelper;
import com.tony.sqlhelper.proxy.ProxySQLObject;

class CacheManager {

    private class ClassCache extends HashMap<Object, Object> {
        private static final long serialVersionUID = -8530404473709890688L;
    }

    private class MemoryCache extends HashMap<Class<?>, ClassCache> {
        private static final long serialVersionUID = -9071736213918349898L;

        @Override
        public ClassCache get(Object key) {
            ClassCache obj = super.get(key);
            if (obj == null) {
                obj = new ClassCache();
                put((Class<?>) key, obj);
            }
            return obj;
        }

    }

    public static class QueueEntry {
        public final Object object;
        public final BiConsumer<Object, Object> operation;
        public final Class<?> clazz;

        public QueueEntry(Class<?> clazz, Object object, BiConsumer<Object, Object> operation) {
            this.object = object;
            this.operation = operation;
            this.clazz = clazz;
        }
    }

    private MemoryCache cachedObjects;

    private HashMap<Integer, MemoryCache> tempCache;

    private Queue<QueueEntry> queuedActions;

    private boolean transactionMode;

    private int state;

    public CacheManager() {
        transactionMode = false;
        state = -1;
        queuedActions = new LinkedList<>();
        tempCache = new HashMap<>();
        cachedObjects = new MemoryCache();
    }

    private BiConsumer<Object, Object> persistObject = (clazz, obj) -> {
        Object key = SQLObjectHelper.getKeyValue(obj);
        cachedObjects.get(clazz).put(key, obj);
    };

    private BiConsumer<Object, Object> removeObject = (clazz, obj) -> {
        Object key = SQLObjectHelper.getKeyValue(obj);
        cachedObjects.get(clazz).remove(key);
    };

    public Object fetchFromTempMemory(Class<?> clazz, Object key){
       
        MemoryCache tempCache = null;
        ClassCache classCache = null;

        do{

            tempCache = this.tempCache.get(this.state);

            if (tempCache == null)
                break;

            
            classCache = tempCache.get(clazz);

            if (classCache == null)
                break;

            Object obj = classCache.get(key);

            if (obj == null)
                break;
            
        }while(false);

        Object obj = fetch(clazz, key, true);

        if(obj == null)
            return null;

        obj = ((ProxySQLObject)obj).cloneObj();

        put(clazz, obj, false);
        return obj;
    }

    public Object fetch(Class<?> clazz, Object key) {
        if (transactionMode) {
            return fetchFromTempMemory(clazz, key);
        } else {
            return fetch(clazz, key, false);
        }
    }

    private Object fetch(Class<?> clazz, Object key, boolean clone) {
        ClassCache classCache = cachedObjects.get(clazz);
        if (classCache == null)
            return null;
        Object obj = classCache.get(key);
        if (!clone || obj == null)
            return obj;
        else
            return ((ProxySQLObject) obj).cloneObj();
    }

    public void put(Class<?> clazz, Object obj) {
        put(clazz, obj, true);
    }

    private void put(Class<?> clazz, Object obj, boolean append) {
        if (transactionMode) {
            if (append)
                queuedActions.add(new QueueEntry(clazz, obj, persistObject));
            MemoryCache tempMemoryCache = tempCache.get(this.state);
            if (tempMemoryCache == null) {
                tempMemoryCache = new MemoryCache();
                tempCache.put(state, tempMemoryCache);
            }
            tempMemoryCache.get(clazz).put(SQLObjectHelper.getKeyValue(obj), obj);
        } else {
            persistObject.accept(clazz, obj);
        }
    }

    public boolean isTransactionMode() {
        return transactionMode;
    }

    public void setTransactionMode(boolean transactionMode) {
        this.transactionMode = transactionMode;
    }

    public void commit() {
        for (QueueEntry q : queuedActions) {
            q.operation.accept(q.clazz, q.object);
        }
        rolllback();
    }

    public void remove(Class<?> clazz, Object object) {
        if (transactionMode) {
            queuedActions.add(new QueueEntry(clazz, object, removeObject));
        } else {
            removeObject.accept(clazz, object);
        }
    }

    public void rolllback() {
        state = 0;
        tempCache.clear();
    }

    public void rolllback(int state) {
        while (this.state > state) {
            tempCache.remove(this.state--);
        }
    }

    public void setState(int state) {
        int prev = this.state;
        this.state = state;
        copyObjToCurrState(prev);
    }

    private void copyObjToCurrState(int fromState) {
        MemoryCache memoryCache = tempCache.get(fromState);
        MemoryCache curMemoryCache = memoryCache;
        if (memoryCache == null)
            return;

        tempCache.put(state, memoryCache);

        memoryCache = new MemoryCache();
        
        ClassCache prevClassCache;
        for (Entry<Class<?>, ClassCache> classCache : curMemoryCache.entrySet()) {
            memoryCache.put(classCache.getKey(), new ClassCache());
            prevClassCache = memoryCache.get(classCache.getKey());
            for (Entry<Object, Object> e : classCache.getValue().entrySet()) {
                prevClassCache.put(e.getKey(), ((ProxySQLObject) e.getValue()).cloneObj());
            }
        }
    }
}