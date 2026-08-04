package com.example.orderservice.entity;

public enum OrderStatus {

    PENDING,

    STOCK_RESERVED,

    PAYMENT_PENDING,

    PAID,

    CONFIRMED,

    SHIPPED,

    DELIVERED,

    CANCELLED,

    PAYMENT_FAILED,

    STOCK_UNAVAILABLE

}