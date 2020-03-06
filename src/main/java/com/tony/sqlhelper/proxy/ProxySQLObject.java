package com.tony.sqlhelper.proxy;

public interface ProxySQLObject extends Cloneable {

    public void setAccessNumber(long accessNumber);

    public long getAccessNumber();

    public Class<?> getEntityClass();

    public ProxySQLObject cloneObj();
}