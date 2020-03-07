package com.tony.sqlhelper.proxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.tony.sqlhelper.SchemaHelper;
import com.tony.sqlhelper.annotation.ManyToOne;
import com.tony.sqlhelper.helper.SQLObjectHelper;

public class ProxyList<T> extends ArrayList<T> {

    public class Change {
        public int action;
        public Object obj;

        @Override
        public boolean equals(Object obj) {
            return this.obj.equals(obj);
        }
    }

    public class ChangeList extends ArrayList<Change> {

        private static final long serialVersionUID = 765143392278436238L;

        @Override
        public boolean add(ProxyList<T>.Change e) {
            if(e == null)
                return false;
            int index = indexOf(e.obj);
            if (index == -1)
                return super.add(e);
            Change c = get(index);
            if (c.action == e.action)
                return false;
            super.remove(index);
            return true;
        }

        @Override
        public int indexOf(Object o) {
            for(int i=0; i<this.size(); i++){
                Object elem = get(i);
                if(elem == null && o == null)
                    return i;
                if(elem != null && elem.equals(o))
                    return i;
            }
            return -1;
        }
    }

    public static byte relationOneToMany = 0;
    public static byte relationManyToMany = 1;
    private Object target;
    private Class<?> clazz;
    private byte relation;
    private ChangeList changes;

    private boolean disablePropagation = false;

    private static final long serialVersionUID = -6586069829979980905L;

    public ProxyList<T> setTarget(Object target, byte relation) {
        this.target = target;
        this.clazz = ((ProxySQLObject) target).getEntityClass();
        this.relation = relation;
        if(this.relation == relationManyToMany)
            changes = new ChangeList();
        return this;
    }

    public ProxyList<T> disablePropagation() {
        disablePropagation = true;
        return this;
    }

    @Override
    public boolean add(T e) {
        if (relation == relationOneToMany)
            addCompleteManyToOne(e);
        else
            addCompleteManyToMany(e);
        disablePropagation = false;
        return super.add(e);
    }

    // filled when calling add on onetomany
    private void addCompleteManyToOne(T object) {
        if (disablePropagation || target == null || object == null)
            return;
        Class<?> objClazz = object instanceof ProxySQLObject ? ((ProxySQLObject) object).getEntityClass()
                : object.getClass();
        Field f = SchemaHelper.getRelatedField(clazz, null, objClazz);

        if (f == null)
            return;
        try {
            f.set(object, target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void addCompleteManyToMany(T object) {
        if (disablePropagation || target == null || object == null)
            return;
        Change c = new Change();
        c.action = 1; //add
        c.obj = object;
        changes.add(c);
        Class<?> objClazz = object instanceof ProxySQLObject ? ((ProxySQLObject) object).getEntityClass()
                : object.getClass();
        Field f = SchemaHelper.getListRelatedField(clazz, null, objClazz);
        if (f == null)
            return;
        Method getter = SchemaHelper.getGetterForField(f, objClazz);
        try {
            List<Object> l = (List<Object>) getter.invoke(object);
            if (l == null) {
                l = new ProxyList<>().setTarget(object, relationManyToMany);
                f.set(object, l);
            }
            ((ProxyList<Object>) l).disablePropagation().add(target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void add(int index, T element) {
        if (relation == relationOneToMany)
            addCompleteManyToOne(element);
        else
            addCompleteManyToMany(element);
        disablePropagation = false;
        super.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        for (T e : c) {
            if (relation == relationOneToMany)
                addCompleteManyToOne(e);
            else
                addCompleteManyToMany(e);
        }
        disablePropagation = false;
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        for (T e : c) {
            if (relation == relationOneToMany)
                addCompleteManyToOne(e);
            else
                addCompleteManyToMany(e);
        }
        disablePropagation = false;
        return super.addAll(index, c);
    }

    @Override
    public void clear() {
        if (relation == relationOneToMany)
            clearCompleteManyToOne();
        else
            clearCompleteManyToMany();
        disablePropagation = false;
        super.clear();
    }

    @Override
    public boolean remove(Object o) {
        int index = indexOf(o);
        if (index == -1)
            return false;
        if (relation == relationOneToMany)
            removeCompleteManyToOne(get(index));
        else
            removeCompleteManyToMany(get(index));
        disablePropagation = false;
        super.remove(index);
        return true;
    }

    private void removeCompleteManyToOne(T object) {
        if (disablePropagation || target == null || object == null)
            return;
        Class<?> objClazz = object instanceof ProxySQLObject ? ((ProxySQLObject) object).getEntityClass()
                : object.getClass();
        Field f = SchemaHelper.getRelatedField(clazz, null, objClazz);

        if (f == null)
            return;
        try {
            f.set(object, null);
            ProxyHandler<Object> h = SQLObjectHelper.getHandler(object);
            h.setMapValueToNull(((ManyToOne) f.getAnnotation(ManyToOne.class)).inverserdBy());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearCompleteManyToOne() {
        if (disablePropagation || target == null)
            return;
        for (T o : this)
            removeCompleteManyToOne(o);

    }

    @SuppressWarnings("unchecked")
    private void removeCompleteManyToMany(T object) {
        if (disablePropagation || target == null || object == null)
            return;
        Change c = new Change();
        c.action = 0; //remove
        c.obj = object;
        changes.add(c);
        Class<?> objClazz = object instanceof ProxySQLObject ? ((ProxySQLObject) object).getEntityClass()
                : object.getClass();
        Field f = SchemaHelper.getListRelatedField(clazz, null, objClazz);

        if (f == null)
            return;
        Method getter = SchemaHelper.getGetterForField(f, objClazz);
        try {
            List<Object> l = (List<Object>) getter.invoke(object);
            if (l == null) {
                l = new ProxyList<>().setTarget(object, relationManyToMany);
                f.set(object, l);
                return;
            }
            ((ProxyList<Object>) l).disablePropagation().remove(target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearCompleteManyToMany() {
        if (disablePropagation)
            return;
        for (T o : this)
            removeCompleteManyToMany(o);
    }

    List<Object> getIdentifiers() {
        List<Object> identifiers = new ArrayList<>();
        for (T o : this) {
            identifiers.add(SQLObjectHelper.getKeyValue(o));
        }
        return identifiers;
    }

    public ChangeList getChanges() {
        return changes;
    }
}