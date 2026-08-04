package com.example.orderservice.event;

import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(

        UUID orderId,

        UUID customerId,

        List<OrderItemEvent> items

) {
}