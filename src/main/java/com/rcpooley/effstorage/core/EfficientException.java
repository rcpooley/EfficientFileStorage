package com.rcpooley.effstorage.core;

public class EfficientException extends Exception {

    public EfficientException(Exception e) {
        super(e);
    }

    public EfficientException(String msg) {
        super(msg);
    }
}
