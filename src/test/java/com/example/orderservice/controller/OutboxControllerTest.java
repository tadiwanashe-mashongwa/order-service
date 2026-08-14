package com.example.orderservice.controller;

import com.example.orderservice.config.SecurityConfig;
import com.example.orderservice.dto.DeadLetteredOutboxEventResponse;
import com.example.orderservice.exception.OutboxEventNotFoundException;
import com.example.orderservice.service.OutboxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OutboxController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class OutboxControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OutboxService outboxService;

    @Test
    void shouldReturnDeadLetteredOutboxEvents() throws Exception {

        UUID eventId = UUID.randomUUID();
        DeadLetteredOutboxEventResponse response =
                new DeadLetteredOutboxEventResponse(
                        eventId,
                        UUID.randomUUID(),
                        "order-created",
                        "OrderCreatedEvent",
                        3,
                        "Kafka is unavailable",
                        Instant.now()
                );
        Page<DeadLetteredOutboxEventResponse> page =
                new PageImpl<>(List.of(response));
        when(outboxService.getDeadLetteredEvents(any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/outbox/dead-lettered"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventId")
                        .value(eventId.toString()))
                .andExpect(jsonPath("$.content[0].attemptCount").value(3))
                .andExpect(jsonPath("$.content[0].lastError")
                        .value("Kafka is unavailable"));

        verify(outboxService).getDeadLetteredEvents(any());
    }

    @Test
    void shouldRequeueDeadLetteredOutboxEvent() throws Exception {

        UUID eventId = UUID.randomUUID();

        mockMvc.perform(post("/api/outbox/{eventId}/requeue", eventId))
                .andExpect(status().isNoContent());

        verify(outboxService).requeueDeadLetteredEvent(eventId);
    }

    @Test
    void shouldReturnNotFoundWhenRequeueingUnknownOutboxEvent() throws Exception {

        UUID eventId = UUID.randomUUID();
        doThrow(new OutboxEventNotFoundException("Outbox event not found"))
                .when(outboxService)
                .requeueDeadLetteredEvent(eventId);

        mockMvc.perform(post("/api/outbox/{eventId}/requeue", eventId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Outbox Event Not Found"));

        verify(outboxService).requeueDeadLetteredEvent(eventId);
    }
}
