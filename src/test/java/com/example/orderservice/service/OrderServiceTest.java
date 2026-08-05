package com.example.orderservice.service;

import com.example.orderservice.client.ApiResponse;
import com.example.orderservice.client.CatalogueClient;
import com.example.orderservice.client.MoneyResponse;
import com.example.orderservice.client.PartResponse;
import com.example.orderservice.dto.CreateOrderItemRequest;
import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.CreateOrderResponse;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.exception.PartNotFoundException;
import com.example.orderservice.mapper.OrderEventMapper;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.producer.OrderEventProducer;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldCreateOrderSuccessfully() {

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

        verify(orderEventProducer)
                .publishOrderCreated(event);

        verifyNoMoreInteractions(orderEventProducer);
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
}