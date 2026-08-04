package com.example.orderservice.exception;

public class PartNotFoundException extends RuntimeException {

    public PartNotFoundException(String message) {
        super(message);
    }

}