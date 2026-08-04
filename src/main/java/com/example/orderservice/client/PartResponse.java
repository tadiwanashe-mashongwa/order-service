package com.example.orderservice.client;

import java.util.UUID;

public record PartResponse(

        UUID partId,

        String name,

        MoneyResponse price

) {
}