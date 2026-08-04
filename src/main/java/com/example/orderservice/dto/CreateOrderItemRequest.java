package com.example.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateOrderItemRequest(

        @NotNull
        UUID partId,

        @NotNull
        @Min(1)
        Integer quantity

) {
}