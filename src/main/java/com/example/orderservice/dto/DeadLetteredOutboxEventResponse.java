package com.example.orderservice.dto;

import java.time.Instant;
import java.util.UUID;

public record DeadLetteredOutboxEventResponse(
        UUID eventId,
        UUID aggregateId,
        String topic,
        String eventType,
        int attemptCount,
        String lastError,
        Instant createdAt
) {
}
