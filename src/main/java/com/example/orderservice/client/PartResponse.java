package com.example.orderservice.client;

import java.util.UUID;

public record PartResponse(

        UUID id,

        String name,

        MoneyResponse price

) {
}