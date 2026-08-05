package com.example.orderservice.dto;

import com.example.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(

        UUID orderId,

        UUID customerId,

        OrderStatus status,

        BigDecimal totalAmount,

        List<OrderItemResponse> items,

        Instant createdAt

) {
}