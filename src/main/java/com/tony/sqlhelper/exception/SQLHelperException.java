package com.tony.sqlhelper.exception;

public class SQLHelperException extends Exception{

    private static final long serialVersionUID = -3798968000414389006L;

    private Exception error;

    public SQLHelperException(String msg){
        super(msg);
    }

    public SQLHelperException(Exception e){
        this.error = e;
    }

    public Exception getError(){
        return error;
    }
}