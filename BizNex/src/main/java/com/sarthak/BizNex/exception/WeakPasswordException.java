package com.sarthak.BizNex.exception;

public class WeakPasswordException extends RuntimeException {
    public WeakPasswordException(String msg) {
        super(msg);
    }
}
