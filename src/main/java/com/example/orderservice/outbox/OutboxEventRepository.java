package com.example.orderservice.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent>
    findTop100ByPublishedFalseAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            Instant nextAttemptAt
    );
}
