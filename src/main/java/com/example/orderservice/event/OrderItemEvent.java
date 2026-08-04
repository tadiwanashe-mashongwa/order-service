package com.example.orderservice.event;

import java.util.UUID;

public record OrderItemEvent(

        UUID partId,

        Integer quantity

) {
}