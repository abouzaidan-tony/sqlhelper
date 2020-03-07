package com.tony.sqlhelper.domain;

import java.util.HashMap;

import com.tony.sqlhelper.annotation.ManyToMany;
import com.tony.sqlhelper.proxy.ProxySQLObject;

public class JoinObject implements ProxySQLObject {

    private static int joinObjCounter = 0;

    public final String joinTable;
    public final HashMap<String, Object> objs;
    private final Object inversedBy;
    public final int tempId;
    private long accessNumber;

    public JoinObject(ManyToMany a, Object referencedBy, Object inversedBy) {
        this.joinTable = a.joinTable();
        objs = new HashMap<>();
        this.inversedBy = inversedBy;
        objs.put(a.referencedBy(), referencedBy);
        objs.put(a.inversedBy(), inversedBy);
        tempId = ++joinObjCounter;
        accessNumber = -1;
    }

    @Override
    public void setAccessNumber(long accessNumber) {
        this.accessNumber = accessNumber;
    }

    @Override
    public long getAccessNumber() {
        return accessNumber;
    }

    @Override
    public Class<?> getEntityClass() {
        return getClass();
    }

    @Override
    public ProxySQLObject cloneObj() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj) {
        return inversedBy.equals(obj);
    }
    
}