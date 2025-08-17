package com.sarthak.BizNex.exception;

public class InsufficientStockException extends RuntimeException{
    public  InsufficientStockException(String message) {
        super(message);
    }
}
