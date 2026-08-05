package com.example.orderservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(

        UUID partId,

        String partName,

        BigDecimal unitPrice,

        Integer quantity,

        BigDecimal subtotal

) {
}