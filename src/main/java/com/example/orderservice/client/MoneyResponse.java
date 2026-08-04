package com.example.orderservice.client;



public record MoneyResponse(
        long amount,
        String currency
) {
}