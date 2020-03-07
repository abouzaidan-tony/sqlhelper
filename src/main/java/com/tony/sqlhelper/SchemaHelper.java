package com.tony.sqlhelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

import com.tony.sqlhelper.helper.SQLHelper.SQLTypes;
import com.tony.sqlhelper.annotation.ManyToMany;
import com.tony.sqlhelper.annotation.ManyToOne;
import com.tony.sqlhelper.annotation.OneToMany;
import com.tony.sqlhelper.annotation.OneToOne;
import com.tony.sqlhelper.annotation.PrimaryKey;
import com.tony.sqlhelper.annotation.Property;
import com.tony.sqlhelper.annotation.Table;
import com.tony.sqlhelper.domain.PropertyMap;
import com.tony.sqlhelper.exception.PrimaryKeyNotFoundException;
import com.tony.sqlhelper.exception.TableNameNotSetException;

public class SchemaHelper {

    private enum CacheKeys {
        Properties, OneToMany, ManyToOne, ManyToMany, OneToOne
    }

    private static HashMap<CacheKeys, HashMap<Class<?>, List<PropertyMap>>> cachedData;
    private static HashMap<Class<?>, String> cachedTableNames;
    private static HashMap<Class<?>, PropertyMap> cachedPrimaryKeys;

    static {
        cachedData = new HashMap<>();
        for (CacheKeys key : CacheKeys.values()) {
            cachedData.put(key, new HashMap<>());
        }
        cachedPrimaryKeys = new HashMap<>();
        cachedTableNames = new HashMap<>();
    }

    public static <T, A extends Annotation> List<PropertyMap> getAnnotatedColumns(Class<T> clazz,
            Class<A> annotation, CacheKeys lookupTable, BiFunction<Field, A, PropertyMap> filler) {
        HashMap<Class<?>, List<PropertyMap>> tbl = cachedData.get(lookupTable);
        if (tbl.containsKey(clazz))
            return tbl.get(clazz);

        List<PropertyMap> list = new ArrayList<>();

        Class<?> tempClass = clazz;
        do {

            for (Field field : tempClass.getDeclaredFields()) {
                field.setAccessible(true);
                if (!field.isAnnotationPresent(annotation))
                    continue;
                A p = (A) field.getAnnotation(annotation);
                PropertyMap map = filler.apply(field, p);
                list.add(map);
            }

            tempClass = tempClass.getSuperclass();

        } while (tempClass != null);

        list = Collections.unmodifiableList(list);
        tbl.put(clazz, list );

        return list;
    }

    private static BiFunction<Field, OneToMany, PropertyMap> OneToManyBiFunction = (f, a) -> {
        return new PropertyMap(f, a.mappedBy(), a.targetEntity());
    };

    private static BiFunction<Field, ManyToMany, PropertyMap> ManyToManyBiFunction = (f, a) -> {
        return new PropertyMap(f, a.referencedBy(), a.inversedBy(), a.joinTable(), a.targetEntity(), SQLTypes.Long);
    };

    private static BiFunction<Field, ManyToOne, PropertyMap> ManyToOneBiFunction = (f, a) -> {
        return new PropertyMap(f, a.inverserdBy(), a.targetEntity(), SchemaHelper.getPrimaryKey(a.targetEntity()).type);
    };

    private static BiFunction<Field, OneToOne, PropertyMap> OneToOneBiFunction = (f, a) -> {
        return new PropertyMap(f, a.inverserdBy(), a.mappedBy(), a.targetEntity(),
                SchemaHelper.getPrimaryKey(a.targetEntity()).type);
    };

    public static <T, A extends Annotation> PropertyMap getXProperty(Class<T> clazz, Field f,
            Class<A> relation, CacheKeys c, BiFunction<Field, A, PropertyMap> filler) {
        List<PropertyMap> lst = getAnnotatedColumns(clazz, relation, c, filler);
        for (PropertyMap p : lst) {
            if (p.field.equals(f))
                return p;
        }
        return null;
    }

    public static <T> PropertyMap getOneToManyProperty(Class<T> clazz, Field f) {
        return getXProperty(clazz, f, OneToMany.class, CacheKeys.OneToMany, OneToManyBiFunction);
    }

    public static <T> PropertyMap getOneToOneProperty(Class<T> clazz, Field f) {
        return getXProperty(clazz, f, OneToOne.class, CacheKeys.OneToOne, OneToOneBiFunction);
    }

    public static <T> PropertyMap getManyToManyProperty(Class<T> clazz, Field f) {
        return getXProperty(clazz, f, ManyToMany.class, CacheKeys.ManyToMany, ManyToManyBiFunction);
    }

    public static <T> PropertyMap getManyToOneProperty(Class<T> clazz, Field f) {
        return getXProperty(clazz, f, ManyToOne.class, CacheKeys.ManyToOne, ManyToOneBiFunction);
    }

    public static <T> List<PropertyMap> getOneToManyColumns(Class<T> clazz) {
        return getAnnotatedColumns(clazz, OneToMany.class, CacheKeys.OneToMany, OneToManyBiFunction);
    }

    public static <T> List<PropertyMap> getManyToOneColumns(Class<T> clazz) {
        return getAnnotatedColumns(clazz, ManyToOne.class, CacheKeys.ManyToOne, ManyToOneBiFunction);
    }

    public static <T> List<PropertyMap> getManyToManyColumns(Class<T> clazz) {
        return getAnnotatedColumns(clazz, ManyToMany.class, CacheKeys.ManyToMany, ManyToManyBiFunction);
    }

    public static <T> List<PropertyMap> getOneToOneColumns(Class<T> clazz) {
        return getAnnotatedColumns(clazz, OneToOne.class, CacheKeys.OneToOne, OneToOneBiFunction);
    }

    public static <T> List<PropertyMap> getCombinedColumns(Class<T> clazz) {
        List<PropertyMap> map = new ArrayList<>(getAnnotatedColumns(clazz, Property.class, CacheKeys.Properties,
                (f, p) -> new PropertyMap(f, p.name(), p.type(), f.isAnnotationPresent(PrimaryKey.class))));
        map.addAll(getManyToOneColumns(clazz));
        map.addAll(getOneToOneColumns(clazz));
        return map;
    }

    public static <T> List<PropertyMap> getColumns(Class<T> clazz) {
        return getAnnotatedColumns(clazz, Property.class, CacheKeys.Properties,
                (f, p) -> new PropertyMap(f, p.name(), p.type(), f.isAnnotationPresent(PrimaryKey.class)));
    }

    public static <T> String getTableName(Class<T> clazz) {
        if (cachedTableNames.containsKey(clazz))
            return cachedTableNames.get(clazz);
        Class<?> tempClass = clazz;
        String tableName = null;

        do {

            if (tempClass.isAnnotationPresent(Table.class)) {
                tableName = tempClass.getAnnotation(Table.class).value();
                break;
            }

            tempClass = tempClass.getSuperclass();
           
        } while (tempClass != null);

        if (tableName == null)
            throw new TableNameNotSetException();
        return tableName;
    }

    public static <T> SQLTypes[] getColumnsType(Class<T> clazz) {
        List<PropertyMap> properties = getCombinedColumns(clazz);
        SQLTypes[] types = new SQLTypes[properties.size()];
        int i = 0;
        for (PropertyMap p : properties) {
            types[i++] = p.type;
        }
        return types;
    }

    public static <T> PropertyMap getPrimaryKey(Class<T> clazz) {
        if (cachedPrimaryKeys.containsKey(clazz))
            return cachedPrimaryKeys.get(clazz);

        Class<?> tempClass = clazz;
        PropertyMap primaryKey = null;
        do {

            for (Field field : tempClass.getDeclaredFields()) {
                field.setAccessible(true);
                if (!field.isAnnotationPresent(PrimaryKey.class))
                    continue;
                Property p = field.getAnnotation(Property.class);
                primaryKey = new PropertyMap(field, p.name(), p.type(), true);
                break;
            }

            tempClass = tempClass.getSuperclass();
           
        } while (tempClass != null);

        if (primaryKey == null)
            throw new PrimaryKeyNotFoundException();
        return primaryKey;
    }

    public static Field getRelatedField(Class<?> type, String name, Class<?> clazz) {

        Field myField = null;

        do {
            for (Field f : clazz.getDeclaredFields()) {
                if (!f.getType().equals(type))
                    continue;
                if (name != null && !name.toLowerCase().endsWith(f.getName().toLowerCase()))
                    continue;
                myField = f;
                myField.setAccessible(true);
                clazz = null;
                break;
            }
            if (clazz != null)
                clazz = clazz.getSuperclass();
        } while (clazz != null);

        return myField;
    }

    public static Method getGetterForField(Field f, Class<?> clazz) {

        Method myMethod = null;

        do {
            for (Method m : clazz.getDeclaredMethods()) {
                if (!(m.getReturnType().equals(f.getType())))
                    continue;
                
                if (!m.getName().toLowerCase().endsWith(f.getName().toLowerCase()))
                    continue;
                myMethod = m;
                myMethod.setAccessible(true);
                clazz = null;
                break;
            }
            if (clazz != null)
                clazz = clazz.getSuperclass();
        } while (clazz != null);

        return myMethod;
    }

    public static Field getListRelatedField(Class<?> type, String name, Class<?> clazz) {

        Field myField = null;

        do {
            for (Field f : clazz.getDeclaredFields()) {
                if(!(f.getGenericType() instanceof ParameterizedType))
                    continue;
                ParameterizedType genType = (ParameterizedType) f.getGenericType();
                Class<?> genClass = (Class<?>) genType.getActualTypeArguments()[0];
                if(!genClass.equals(type))
                    continue;
                if (name != null && !name.toLowerCase().endsWith(f.getName().toLowerCase()))
                    continue;
                myField = f;
                myField.setAccessible(true);
                clazz = null;
                break;
            }
            if (clazz != null)
                clazz = clazz.getSuperclass();
        } while (clazz != null);

        return myField;
    }
}