package com.example.orderservice.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(

        UUID orderId,

        UUID customerId,

        BigDecimal totalAmount,

        List<OrderItemEvent> items

) {
}
