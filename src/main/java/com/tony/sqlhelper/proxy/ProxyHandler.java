package com.tony.sqlhelper.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.tony.sqlhelper.EntityManager;
import com.tony.sqlhelper.SchemaHelper;
import com.tony.sqlhelper.annotation.*;
import com.tony.sqlhelper.domain.PropertyMap;
import com.tony.sqlhelper.exception.SetListException;
import com.tony.sqlhelper.helper.RepositoryHelper;
import com.tony.sqlhelper.helper.SQLObjectHelper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javassist.util.proxy.MethodHandler;

public class ProxyHandler<T> implements MethodHandler {

    private static Logger log = LogManager.getLogger(EntityManager.class);
    
    private EntityManager em;
    private Class<T> clazz;
    private HashMap<String, Object> map;
    private HashMap<String, Object> cloningMap;
    private T target;
    private long accessNumber;

    private static int staticTempId = 0;

    private int tempId;

    public ProxyHandler(Class<T> clazz, HashMap<String, Object> map, EntityManager em) {
        this.clazz = clazz;
        this.map = map;
        this.em = em;
        this.cloningMap = new HashMap<>();
        accessNumber = -1;
        tempId = ++staticTempId;
    }

    private Object handleProxyObjectFunctions(Object self, Method thisMethod, Object[] args) {
        switch (thisMethod.getName()) {
            case "getEntityClass":
                return clazz;
            case "setAccessNumber":
                accessNumber = (long) args[0];
                return null;
            case "getAccessNumber":
                return accessNumber;
            case "cloneObj":
                return cloneObj();
        }
        return null;
    }

    @Override
    public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {

        if (proceed == null)
            return handleProxyObjectFunctions(self, thisMethod, args);

        if (thisMethod.getName().startsWith("get") && thisMethod.getParameterCount() == 0) {
            return overrideGetters(self, thisMethod, proceed, args);
        }

        if (thisMethod.getName().startsWith("set") && thisMethod.getParameterCount() == 1) {
            return overrideSetters(self, thisMethod, proceed, args);
        }

        if ("equals".equals(thisMethod.getName()) && thisMethod.getParameterCount() == 1) {
            return overrideEquals(self, thisMethod, proceed, args);
        }

        return proceed.invoke(self, args);
    }

    private Object overrideEquals(Object self, Method thisMethod, Method proceed, Object[] args)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        if(args[0] == null)
                return false;
        if (self.getClass().equals(args[0].getClass())) {
            Object key1 = SQLObjectHelper.getKeyValue(self);
            ProxyHandler<Object> proxy = SQLObjectHelper.getHandler(args[0]);
            Object key2 = SQLObjectHelper.getKeyValue(args[0]);
            if (key1 != null)
                return key1.equals(key2);
            else if (key1 == null && key2 == null)
                return new Boolean(tempId == proxy.tempId);
            return false;
        } else {
            Object key1 = SQLObjectHelper.getKeyValue(self);
            if (key1.getClass().equals(args[0].getClass())) {
                return key1.equals(args[0]);
            }
        }

        return proceed.invoke(self, args);
    }

    @SuppressWarnings("unchecked")
    private Object overrideGetters(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
        Object ret = proceed.invoke(self, args);
        if (ret != null)
            return ret;

        Field f = SchemaHelper.getRelatedField(proceed.getReturnType(), proceed.getName(), clazz);

        if (f == null) {
            return null;
        }

        Object cachedObj = cloningMap.get(f.getName());

        if(cachedObj != null){
            return handleCacheRetrieval(self, f, cachedObj);
        }

        Annotation[] annotations = f.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof ManyToMany) {
                RepositoryHelper.fillManyToManyReferences(em, (T) self, SchemaHelper.getManyToManyProperty(clazz, f));
            } else if (annotation instanceof ManyToOne) {
                RepositoryHelper.fillManyToOneReferences(em, (T) self, SchemaHelper.getManyToOneProperty(clazz, f));
            } else if (annotation instanceof OneToMany) {
                RepositoryHelper.fillOneToManyReferences(em, (T) self, SchemaHelper.getOneToManyProperty(clazz, f));
            } else if (annotation instanceof OneToOne) {
                RepositoryHelper.fillOneToOneReferences(em, (T) self, SchemaHelper.getOneToOneProperty(clazz, f));
            }
        }
        return proceed.invoke(self, args);
    }

    @SuppressWarnings("unchecked")
    private Object handleCacheRetrieval(Object self, Field f, Object cache)
            throws IllegalArgumentException, IllegalAccessException {
        try{
            Annotation[] annotations = f.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof ManyToMany) {
                    fillListFromCache(self, f, ((ManyToMany) annotation).targetEntity(), (List<Object>) cache);
                } else if (annotation instanceof ManyToOne) {
                    f.set(self, em.GetRepository(((OneToOne) annotation).targetEntity()).find(cache));
                } else if (annotation instanceof OneToMany) {
                    fillListFromCache(self, f, ((OneToMany) annotation).targetEntity(), (List<Object>) cache);
                } else if (annotation instanceof OneToOne) {
                    f.set(self, em.GetRepository(((OneToOne)annotation).targetEntity()).find(cache));
                }
            }

            return f.get(self);
        }catch(Exception ex){
            ex.printStackTrace();
        }

        return null;
        
    }

    private void fillListFromCache(Object self, Field f, Class<?> targetEntity, List<Object> identifiers)
            throws IllegalArgumentException, IllegalAccessException {
        ProxyList<Object> l = new ProxyList<>().setTarget(self, ProxyList.relationManyToMany);
        f.set(self, l);
        for (Object o : identifiers) {
            l.disablePropagation().add(em.GetRepository(targetEntity).find(o));
        }
    }

    private Object overrideSetters(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
        Parameter param = thisMethod.getParameters()[0];

        if (param.getType().isInstance(List.class)) {
            throw new SetListException();
        }

        if (!SQLObjectHelper.isEntity(param.getType()))
            return proceed.invoke(self, args);

        Field f = SchemaHelper.getRelatedField(param.getType(), proceed.getName(), clazz);

        if (f != null)
            setParameterValue(self, f, args[0]);

        return proceed.invoke(self, args);
    }

    private void setParameterValue(Object self, Field f, Object val)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        for (Annotation a : f.getAnnotations()) {
            if (a instanceof OneToOne) {
                handleOneToOneRelation(self, f, val, (OneToOne) a);
            } else if (a instanceof ManyToOne) {
                handleManyToOneRelation(self, f, val, (ManyToOne) a);
            }
        }
    }

    private <U> void handleOneToOneRelation(Object self, Field f, Object val, OneToOne a)
            throws IllegalArgumentException, IllegalAccessException {
        if (val != null) {
            Field f2 = SchemaHelper.getRelatedField(clazz, null, a.targetEntity());
            f2.setAccessible(true);
            f2.set(val, self);
        } else if (a.mappedBy().length() == 0) {
            setMapValueToNull(a.inverserdBy());
        } else {
            ProxyHandler<Object> handler2 = SQLObjectHelper.getHandler(val);
            handler2.setMapValueToNull(a.mappedBy());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleManyToOneRelation(Object self, Field f, Object val, ManyToOne a)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Field f2 = SchemaHelper.getListRelatedField(clazz, null, a.targetEntity());
        f2.setAccessible(true);
        Method getter = SchemaHelper.getGetterForField(f2, a.targetEntity());
        if (val != null) {
            List<Object> l = (List<Object>) getter.invoke(val);
            if (l == null) {
                l = new ProxyList<>().setTarget(val, ProxyList.relationOneToMany);
                f2.set(val, l);
            }
            ((ProxyList<Object>) l).disablePropagation().add(self);
        } else {
            setMapValueToNull(a.inverserdBy());
            Object extVal = f.get(self);
            if (extVal != null) {
                List<Object> l = (List<Object>) getter.invoke(extVal);
                if (l == null)
                    return;
                ((ProxyList<Object>) l).disablePropagation().remove(self);
            }
        }
    }

    void setMapValueToNull(String columnName) {
        if (map != null)
            map.put(columnName, null);
    }

    private void init() throws Exception {
        if (map == null)
            return;
        Field f = null;
        for (PropertyMap var : SchemaHelper.getColumns(clazz)) {
            String column = var.columnName;
            if (map.containsKey(column)) {
                f = var.field;
                f.setAccessible(true);
                f.set(target, map.get(column));
            }
        }
    }

    public void setTarget(T target) throws Exception {
        this.target = target;
        init();
    }

    public HashMap<String, Object> getMap() {
        return map;
    }

    public void initMap() {
        if (map == null)
            map = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Object cloneObj() {
        try {
            T proxyInstance = EntityManager.GetInstance().GetRepository(clazz).create();

            ProxyHandler<T> handler = SQLObjectHelper.getHandler(proxyInstance);
            handler.map = cloneMap();
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                Object o = f.get(target);
                if (o instanceof ProxySQLObject) {
                    log.trace("Cloning from class [" + clazz.getCanonicalName() + "] object ...");
                    handler.cloningMap.put(f.getName(), SQLObjectHelper.getKeyValue(o));
                } else if (o instanceof ProxyList) {
                    log.trace("Cloning from class [" + clazz.getCanonicalName() + "] identifiers ...");
                    handler.cloningMap.put(f.getName(), ((ProxyList<Object>)o).getIdentifiers());
                } else if(o != null){
                    log.trace("Cloning from class [" + clazz.getCanonicalName() + "] value [" + o +"]");
                    f.set(proxyInstance, o);
                }
            }
            return proxyInstance;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private HashMap<String, Object> cloneMap() {
        HashMap<String, Object> m = new HashMap<>();
        for (Entry<String, Object> e : map.entrySet()) {
            m.put(e.getKey(), e.getValue());
        }
        return m;
    }
}