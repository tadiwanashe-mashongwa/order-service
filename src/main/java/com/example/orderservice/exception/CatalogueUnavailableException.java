package com.example.orderservice.exception;

public class CatalogueUnavailableException extends RuntimeException {

    public CatalogueUnavailableException(String message) {
        super(message);
    }

}