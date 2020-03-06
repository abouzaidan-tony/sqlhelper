package com.tony.sqlhelper.exception;

public class SetListException extends SQLHelperException {

    private static final long serialVersionUID = 1L;

    public SetListException() {
        super("Cannot set List member of Entity Class");
    }

}