package com.sarthak.BizNex.exception;

public class CustomerInformationMissingInBillException extends RuntimeException{
    public CustomerInformationMissingInBillException(String message) {
        super(message);
    }
}
