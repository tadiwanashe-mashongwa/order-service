package com.example.orderservice.service;

import com.example.orderservice.dto.DeadLetteredOutboxEventResponse;
import com.example.orderservice.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional(readOnly = true)
    public Page<DeadLetteredOutboxEventResponse> getDeadLetteredEvents(
            Pageable pageable
    ) {
        return outboxEventRepository.findByDeadLetteredTrue(pageable)
                .map(event -> new DeadLetteredOutboxEventResponse(
                        event.getId(),
                        event.getAggregateId(),
                        event.getTopic(),
                        event.getEventType(),
                        event.getAttemptCount(),
                        event.getLastError(),
                        event.getCreatedAt()
                ));
    }
}
