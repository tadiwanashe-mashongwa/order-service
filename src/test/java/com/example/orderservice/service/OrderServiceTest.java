package com.example.orderservice.service;

import com.example.orderservice.client.ApiResponse;
import com.example.orderservice.client.CatalogueClient;
import com.example.orderservice.client.MoneyResponse;
import com.example.orderservice.client.PartResponse;
import com.example.orderservice.dto.*;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.exception.InvalidOrderStatusTransitionException;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.exception.PartNotFoundException;
import com.example.orderservice.mapper.OrderEventMapper;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.outbox.OutboxEventRepository;
import com.example.orderservice.outbox.OutboxEvent;
import com.example.orderservice.producer.OrderEventProducer;
import com.example.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CatalogueClient catalogueClient;

    @Mock
    private OrderEventMapper orderEventMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderEventProducer orderEventProducer;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {

        // Arrange
        UUID customerId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        Instant createdAt = Instant.now();

        CreateOrderRequest request =
                new CreateOrderRequest(
                        customerId,
                        List.of(
                                new CreateOrderItemRequest(
                                        partId,
                                        2
                                )
                        )
                );

        PartResponse partResponse =
                new PartResponse(
                        partId,
                        "Brake Pad",
                        new MoneyResponse(
                                120,
                                "USD"
                        )
                );

        when(catalogueClient.getPartById(partId))
                .thenReturn(
                        new ApiResponse<>(
                                true,
                                "Success",
                                partResponse,
                                Instant.now()
                        )
                );

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {

                    Order order = invocation.getArgument(0);

                    order.setId(orderId);
                    order.setCreatedAt(createdAt);

                    return order;
                });

        OrderCreatedEvent event =
                new OrderCreatedEvent(
                        orderId,
                        customerId,
                        List.of()
                );

        when(orderEventMapper.toOrderCreatedEvent(any(Order.class)))
                .thenReturn(event);

        when(objectMapper.writeValueAsString(event))
                .thenReturn("{}");

        // Act
        CreateOrderResponse response =
                orderService.createOrder(request);

        // Assert
        assertNotNull(response);

        assertEquals(orderId, response.orderId());
        assertEquals(customerId, response.customerId());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(
                new BigDecimal("240"),
                response.totalAmount()
        );
        assertEquals(createdAt, response.createdAt());

        ArgumentCaptor<Order> captor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository).save(captor.capture());

        Order savedOrder = captor.getValue();

        assertEquals(customerId, savedOrder.getCustomerId());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());
        assertEquals(
                new BigDecimal("240"),
                savedOrder.getTotalAmount()
        );

        assertEquals(1, savedOrder.getItems().size());

        assertEquals(
                partId,
                savedOrder.getItems().getFirst().getPartId()
        );

        assertEquals(
                2,
                savedOrder.getItems().getFirst().getQuantity()
        );

        verify(orderEventMapper)
                .toOrderCreatedEvent(savedOrder);

        verifyNoInteractions(orderEventProducer);
    }
    @Test
    void shouldThrowPartNotFoundExceptionWhenPartDoesNotExist() {

        // Arrange
        UUID customerId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();

        CreateOrderRequest request =
                new CreateOrderRequest(
                        customerId,
                        List.of(
                                new CreateOrderItemRequest(
                                        partId,
                                        1
                                )
                        )
                );

        when(catalogueClient.getPartById(partId))
                .thenThrow(new PartNotFoundException("Part not found"));

        // Act + Assert
        assertThrows(
                PartNotFoundException.class,
                () -> orderService.createOrder(request)
        );

        verify(orderRepository, never()).save(any());

        verify(orderEventMapper, never())
                .toOrderCreatedEvent(any());

        verify(orderEventProducer, never())
                .publishOrderCreated(any());
    }
    @Test
    void shouldReturnOrderWhenOrderExists() {

        // Arrange
        UUID orderId = UUID.randomUUID();

        Order order = Order.builder()
                .id(orderId)
                .build();

        OrderResponse response = mock(OrderResponse.class);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(orderMapper.toOrderResponse(order))
                .thenReturn(response);

        // Act
        OrderResponse result = orderService.getOrderById(orderId);

        // Assert
        assertSame(response, result);

        verify(orderRepository).findById(orderId);
        verify(orderMapper).toOrderResponse(order);

        verifyNoMoreInteractions(
                orderRepository,
                orderMapper
        );
    }
    @Test
    void shouldThrowOrderNotFoundExceptionWhenOrderDoesNotExist() {

        // Arrange
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById(orderId)
        );

        verify(orderRepository).findById(orderId);

        verify(orderMapper, never())
                .toOrderResponse(any());

        verifyNoMoreInteractions(orderRepository);
    }
    @Test
    void shouldReturnAllOrdersWhenStatusIsNull() {

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .build();

        OrderSummaryResponse response = mock(OrderSummaryResponse.class);

        Page<Order> page =
                new PageImpl<>(List.of(order));

        when(orderRepository.findAll(pageable))
                .thenReturn(page);

        when(orderMapper.toOrderSummaryResponse(order))
                .thenReturn(response);

        // Act
        Page<OrderSummaryResponse> result =
                orderService.getAllOrders(null, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertSame(response, result.getContent().getFirst());

        verify(orderRepository).findAll(pageable);

        verify(orderRepository, never())
                .findByStatus(any(), any());

        verify(orderMapper)
                .toOrderSummaryResponse(order);

        verifyNoMoreInteractions(
                orderRepository,
                orderMapper
        );
    }
    @Test
    void shouldReturnOrdersFilteredByStatus() {

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .build();

        OrderSummaryResponse response = mock(OrderSummaryResponse.class);

        Page<Order> page =
                new PageImpl<>(List.of(order));

        when(orderRepository.findByStatus(
                OrderStatus.PENDING,
                pageable))
                .thenReturn(page);

        when(orderMapper.toOrderSummaryResponse(order))
                .thenReturn(response);

        // Act
        Page<OrderSummaryResponse> result =
                orderService.getAllOrders(
                        OrderStatus.PENDING,
                        pageable
                );

        // Assert
        assertEquals(1, result.getTotalElements());
        assertSame(response, result.getContent().getFirst());

        verify(orderRepository)
                .findByStatus(
                        OrderStatus.PENDING,
                        pageable
                );

        verify(orderRepository, never())
                .findAll(any(Pageable.class));

        verify(orderMapper)
                .toOrderSummaryResponse(order);

        verifyNoMoreInteractions(
                orderRepository,
                orderMapper
        );
    }
    @Test
    void shouldReturnOrdersForCustomer() {

        // Arrange
        UUID customerId = UUID.randomUUID();

        Pageable pageable = PageRequest.of(0, 10);

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .build();

        OrderSummaryResponse response = mock(OrderSummaryResponse.class);

        Page<Order> page = new PageImpl<>(List.of(order));

        when(orderRepository.findByCustomerId(customerId, pageable))
                .thenReturn(page);

        when(orderMapper.toOrderSummaryResponse(order))
                .thenReturn(response);

        // Act
        Page<OrderSummaryResponse> result =
                orderService.getOrdersByCustomer(
                        customerId,
                        pageable
                );

        // Assert
        assertEquals(1, result.getTotalElements());
        assertSame(response, result.getContent().getFirst());

        verify(orderRepository)
                .findByCustomerId(customerId, pageable);

        verify(orderMapper)
                .toOrderSummaryResponse(order);

        verifyNoMoreInteractions(
                orderRepository,
                orderMapper
        );
    }
    @Test
    void shouldReturnEmptyPageWhenCustomerHasNoOrders() {

        // Arrange
        UUID customerId = UUID.randomUUID();

        Pageable pageable = PageRequest.of(0, 10);

        Page<Order> page = Page.empty(pageable);

        when(orderRepository.findByCustomerId(customerId, pageable))
                .thenReturn(page);

        // Act
        Page<OrderSummaryResponse> result =
                orderService.getOrdersByCustomer(
                        customerId,
                        pageable
                );

        // Assert
        assertTrue(result.isEmpty());

        verify(orderRepository)
                .findByCustomerId(customerId, pageable);

        verify(orderMapper, never())
                .toOrderSummaryResponse(any());

        verifyNoMoreInteractions(
                orderRepository,
                orderMapper
        );
    }
    @Test
    void shouldTransitionOrderStatusSuccessfully() {

        // Arrange
        UUID orderId = UUID.randomUUID();

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        // Act
        orderService.transitionOrderStatus(
                orderId,
                OrderStatus.STOCK_RESERVED
        );

        // Assert
        assertEquals(
                OrderStatus.STOCK_RESERVED,
                order.getStatus()
        );

        verify(orderRepository).findById(orderId);

        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void shouldPersistOrderStatusChangedEventForValidTransition() throws Exception {

        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"previousStatus\":\"PENDING\",\"status\":\"STOCK_RESERVED\"}");

        orderService.transitionOrderStatus(
                orderId,
                OrderStatus.STOCK_RESERVED
        );

        ArgumentCaptor<OutboxEvent> eventCaptor =
                ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());

        OutboxEvent outboxEvent = eventCaptor.getValue();
        assertEquals(orderId, outboxEvent.getAggregateId());
        assertEquals("order-status-changed", outboxEvent.getTopic());
        assertEquals("OrderStatusChangedEvent", outboxEvent.getEventType());
        assertFalse(outboxEvent.isPublished());
    }
    @Test
    void shouldThrowExceptionForInvalidStatusTransition() {

        // Arrange
        UUID orderId = UUID.randomUUID();

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.DELIVERED)
                .build();

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        // Act + Assert
        assertThrows(
                InvalidOrderStatusTransitionException.class,
                () -> orderService.transitionOrderStatus(
                        orderId,
                        OrderStatus.PENDING
                )
        );

        verify(orderRepository).findById(orderId);

        verifyNoInteractions(outboxEventRepository);

        verifyNoMoreInteractions(orderRepository);
    }
}
