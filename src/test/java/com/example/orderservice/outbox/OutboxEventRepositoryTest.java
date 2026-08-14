package com.example.orderservice.outbox;

import com.example.orderservice.config.AbstractPostgresContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class OutboxEventRepositoryTest extends AbstractPostgresContainerTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void shouldFindOnlyUnpublishedEventsWhoseRetryTimeIsDue() {

        Instant now = Instant.now();
        OutboxEvent dueEvent = createEvent(now.minusSeconds(1));
        OutboxEvent futureEvent = createEvent(now.plusSeconds(60));
        OutboxEvent deadLetteredEvent = OutboxEvent.builder()
                .aggregateId(UUID.randomUUID())
                .topic("order-created")
                .eventType("OrderCreatedEvent")
                .payload("{}")
                .nextAttemptAt(now.minusSeconds(1))
                .deadLettered(true)
                .build();

        outboxEventRepository.saveAll(
                List.of(dueEvent, futureEvent, deadLetteredEvent)
        );

        List<OutboxEvent> result = outboxEventRepository
                .findTop100ByPublishedFalseAndDeadLetteredFalseAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        now
                );

        assertEquals(List.of(dueEvent.getId()),
                result.stream().map(OutboxEvent::getId).toList());
    }

    private OutboxEvent createEvent(Instant nextAttemptAt) {
        return OutboxEvent.builder()
                .aggregateId(UUID.randomUUID())
                .topic("order-created")
                .eventType("OrderCreatedEvent")
                .payload("{}")
                .nextAttemptAt(nextAttemptAt)
                .build();
    }
}
