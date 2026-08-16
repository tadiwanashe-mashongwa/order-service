package com.example.orderservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentStatusChangedEvent(
        UUID paymentId,
        UUID orderId,
        String status
) {
}
