package com.example.orderservice.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String payload;

    @Builder.Default
    @Column(nullable = false)
    private boolean published = false;

    @Builder.Default
    @Column(nullable = false)
    private int attemptCount = 0;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Builder.Default
    @Column(nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    public void markPublished() {
        published = true;
    }

    public void recordFailure(Exception exception) {
        attemptCount++;
        lastError = exception.getCause() == null
                ? exception.getMessage()
                : exception.getCause().getMessage();
        nextAttemptAt = Instant.now().plusSeconds(1);
    }
}
