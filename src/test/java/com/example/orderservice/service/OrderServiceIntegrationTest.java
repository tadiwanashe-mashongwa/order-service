package com.example.orderservice.service;

import com.example.orderservice.client.ApiResponse;
import com.example.orderservice.client.CatalogueClient;
import com.example.orderservice.client.MoneyResponse;
import com.example.orderservice.client.PartResponse;
import com.example.orderservice.config.AbstractPostgresContainerTest;
import com.example.orderservice.dto.CreateOrderItemRequest;
import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.CreateOrderResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.outbox.OutboxEvent;
import com.example.orderservice.outbox.OutboxEventRepository;
import com.example.orderservice.exception.CatalogueUnavailableException;
import com.example.orderservice.exception.PartNotFoundException;
import com.example.orderservice.producer.OrderEventProducer;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Transactional
class OrderServiceIntegrationTest extends AbstractPostgresContainerTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private CatalogueClient catalogueClient;

    @MockitoBean
    private OrderEventProducer orderEventProducer;

    @Test
    void shouldCreateOrderSuccessfully() {

        // Given
        CreateOrderRequest request = createRequest();

        when(catalogueClient.getPartById(any()))
                .thenReturn(createCatalogueResponse());

        // When
        CreateOrderResponse response =
                orderService.createOrder(request);

        // Then
        assertNotNull(response.orderId());
        assertEquals(request.customerId(), response.customerId());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(
                new BigDecimal("250"),
                response.totalAmount()
        );

        Order persisted =
                orderRepository.findById(response.orderId())
                        .orElseThrow();

        assertEquals(
                OrderStatus.PENDING,
                persisted.getStatus()
        );

        assertEquals(
                new BigDecimal("250"),
                persisted.getTotalAmount()
        );

        assertEquals(
                1,
                persisted.getItems().size()
        );

        verify(catalogueClient)
                .getPartById(any());

        verifyNoInteractions(orderEventProducer);
    }

    @Test
    void shouldPersistPendingOrderCreatedEventInOutbox() {

        // Given
        CreateOrderRequest request = createRequest();

        when(catalogueClient.getPartById(any()))
                .thenReturn(createCatalogueResponse());

        // When
        CreateOrderResponse response = orderService.createOrder(request);

        // Then
        OutboxEvent outboxEvent = outboxEventRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        assertEquals(response.orderId(), outboxEvent.getAggregateId());
        assertEquals("order-created", outboxEvent.getTopic());
        assertEquals("OrderCreatedEvent", outboxEvent.getEventType());
        assertFalse(outboxEvent.isPublished());
        assertTrue(outboxEvent.getPayload().contains(response.orderId().toString()));
    }

    @Test
    void shouldReturnTheOriginalOrderWhenAnIdempotencyKeyIsRepeated() {

        CreateOrderRequest request = createRequest();
        String idempotencyKey = UUID.randomUUID().toString();

        when(catalogueClient.getPartById(any()))
                .thenReturn(createCatalogueResponse());

        CreateOrderResponse first = orderService.createOrder(request, idempotencyKey);
        CreateOrderResponse repeated = orderService.createOrder(request, idempotencyKey);

        assertEquals(first.orderId(), repeated.orderId());
        assertEquals(1, orderRepository.count());
        assertEquals(1, outboxEventRepository.count());
        verify(catalogueClient, times(1)).getPartById(any());
    }

    @Test
    void shouldThrowPartNotFoundWhenCatalogueReturns404() {

        // Given
        CreateOrderRequest request = createRequest();

        when(catalogueClient.getPartById(any()))
                .thenThrow(
                        new PartNotFoundException(
                                "Part not found"
                        )
                );

        // When + Then
        assertThrows(
                PartNotFoundException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals(
                0,
                orderRepository.count()
        );

        verify(orderEventProducer, never())
                .publishOrderCreated(any());
    }

    private CreateOrderRequest createRequest() {

        return new CreateOrderRequest(
                UUID.randomUUID(),
                List.of(
                        new CreateOrderItemRequest(
                                UUID.randomUUID(),
                                1
                        )
                )
        );
    }

    private ApiResponse<PartResponse> createCatalogueResponse() {

        PartResponse part =
                new PartResponse(
                        UUID.randomUUID(),
                        "Brake Pads",
                        new MoneyResponse(
                                250,
                                "USD"
                        )
                );

        return new ApiResponse<>(
                true,
                "Success",
                part,
                Instant.now()
        );
    }
    @Test
    void shouldThrowCatalogueUnavailableWhenCatalogueIsDown() {

        // Given
        CreateOrderRequest request = createRequest();

        when(catalogueClient.getPartById(any()))
                .thenThrow(
                        new CatalogueUnavailableException(
                                "Catalogue unavailable"
                        )
                );

        // When + Then
        assertThrows(
                CatalogueUnavailableException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals(
                0,
                orderRepository.count()
        );

        verify(orderEventProducer, never())
                .publishOrderCreated(any());
    }
    @Test
    void shouldCreateOrderWithMultipleItems() {

        // Given
        UUID brakePadsId = UUID.randomUUID();
        UUID oilFilterId = UUID.randomUUID();

        CreateOrderRequest request =
                new CreateOrderRequest(
                        UUID.randomUUID(),
                        List.of(
                                new CreateOrderItemRequest(
                                        brakePadsId,
                                        2
                                ),
                                new CreateOrderItemRequest(
                                        oilFilterId,
                                        3
                                )
                        )
                );

        when(catalogueClient.getPartById(brakePadsId))
                .thenReturn(
                        createCatalogueResponse(
                                brakePadsId,
                                "Brake Pads",
                                250
                        )
                );

        when(catalogueClient.getPartById(oilFilterId))
                .thenReturn(
                        createCatalogueResponse(
                                oilFilterId,
                                "Oil Filter",
                                100
                        )
                );

        // When
        CreateOrderResponse response =
                orderService.createOrder(request);

        // Then
        Order persisted =
                orderRepository.findById(response.orderId())
                        .orElseThrow();

        assertEquals(
                new BigDecimal("800"),
                persisted.getTotalAmount()
        );

        assertEquals(
                2,
                persisted.getItems().size()
        );

        verify(catalogueClient, times(2))
                .getPartById(any());

        verifyNoInteractions(orderEventProducer);
    }
    private ApiResponse<PartResponse> createCatalogueResponse(
            UUID partId,
            String partName,
            long amount
    ) {

        PartResponse part =
                new PartResponse(
                        partId,
                        partName,
                        new MoneyResponse(
                                amount,
                                "USD"
                        )
                );

        return new ApiResponse<>(
                true,
                "Success",
                part,
                Instant.now()
        );
    }
}
