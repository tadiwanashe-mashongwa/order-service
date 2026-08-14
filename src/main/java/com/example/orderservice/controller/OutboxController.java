package com.example.orderservice.controller;

import com.example.orderservice.dto.DeadLetteredOutboxEventResponse;
import com.example.orderservice.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@RestController
@RequestMapping("/api/outbox")
@RequiredArgsConstructor
public class OutboxController {

    private final OutboxService outboxService;

    @GetMapping("/dead-lettered")
    public Page<DeadLetteredOutboxEventResponse> getDeadLetteredEvents(
            @ParameterObject
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        return outboxService.getDeadLetteredEvents(pageable);
    }

    @PostMapping("/{eventId}/requeue")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requeueDeadLetteredEvent(@PathVariable UUID eventId) {
        outboxService.requeueDeadLetteredEvent(eventId);
    }
}
