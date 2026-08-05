package com.example.orderservice.mapper;

import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.OrderSummaryResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderMapperTest {

    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        orderMapper = new OrderMapper();
    }

    @Test
    void shouldMapOrderToOrderResponse() {

        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();

        Instant createdAt = Instant.now();

        Order order = Order.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("250.00"))
                .createdAt(createdAt)
                .build();

        OrderItem item = OrderItem.builder()
                .partId(partId)
                .partName("Brake Pad")
                .unitPrice(new BigDecimal("125.00"))
                .quantity(2)
                .subtotal(new BigDecimal("250.00"))
                .build();

        order.addItem(item);

        // Act
        OrderResponse response = orderMapper.toOrderResponse(order);

        // Assert
        assertNotNull(response);
        assertEquals(orderId, response.orderId());
        assertEquals(customerId, response.customerId());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(new BigDecimal("250.00"), response.totalAmount());
        assertEquals(createdAt, response.createdAt());

        assertEquals(1, response.items().size());

        assertEquals(partId, response.items().getFirst().partId());
        assertEquals("Brake Pad", response.items().getFirst().partName());
        assertEquals(new BigDecimal("125.00"), response.items().getFirst().unitPrice());
        assertEquals(2, response.items().getFirst().quantity());
        assertEquals(new BigDecimal("250.00"), response.items().getFirst().subtotal());
    }

    @Test
    void shouldMapOrderToOrderResponseWithEmptyItems() {

        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Instant createdAt = Instant.now();

        Order order = Order.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(BigDecimal.ZERO)
                .createdAt(createdAt)
                .items(List.of())
                .build();

        // Act
        OrderResponse response = orderMapper.toOrderResponse(order);

        // Assert
        assertNotNull(response);
        assertTrue(response.items().isEmpty());
        assertEquals(orderId, response.orderId());
        assertEquals(customerId, response.customerId());
        assertEquals(OrderStatus.CONFIRMED, response.status());
        assertEquals(BigDecimal.ZERO, response.totalAmount());
        assertEquals(createdAt, response.createdAt());
    }

    @Test
    void shouldMapOrderToOrderSummaryResponse() {

        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Instant createdAt = Instant.now();

        Order order = Order.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.SHIPPED)
                .totalAmount(new BigDecimal("980.50"))
                .createdAt(createdAt)
                .build();

        // Act
        OrderSummaryResponse response =
                orderMapper.toOrderSummaryResponse(order);

        // Assert
        assertNotNull(response);
        assertEquals(orderId, response.orderId());
        assertEquals(customerId, response.customerId());
        assertEquals(OrderStatus.SHIPPED, response.status());
        assertEquals(new BigDecimal("980.50"), response.totalAmount());
        assertEquals(createdAt, response.createdAt());
    }

    @Test
    void shouldMapOrderSummaryResponseWithCancelledStatus() {

        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Instant createdAt = Instant.now();

        Order order = Order.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.CANCELLED)
                .totalAmount(new BigDecimal("1500.00"))
                .createdAt(createdAt)
                .build();

        // Act
        OrderSummaryResponse response =
                orderMapper.toOrderSummaryResponse(order);

        // Assert
        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.status());
        assertEquals(new BigDecimal("1500.00"), response.totalAmount());
        assertEquals(orderId, response.orderId());
        assertEquals(customerId, response.customerId());
        assertEquals(createdAt, response.createdAt());
    }
}