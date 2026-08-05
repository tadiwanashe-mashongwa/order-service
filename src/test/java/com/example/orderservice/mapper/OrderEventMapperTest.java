package com.example.orderservice.mapper;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.event.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderEventMapperTest {

    private OrderEventMapper orderEventMapper;

    @BeforeEach
    void setUp() {
        orderEventMapper = new OrderEventMapper();
    }

    @Test
    void shouldMapOrderToOrderCreatedEventWithSingleItem() {

        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();

        Order order = Order.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(120.00))
                .build();

        OrderItem item = OrderItem.builder()
                .partId(partId)
                .partName("Brake Pad")
                .unitPrice(BigDecimal.valueOf(120.00))
                .quantity(1)
                .subtotal(BigDecimal.valueOf(120.00))
                .build();

        order.addItem(item);

        // Act
        OrderCreatedEvent event =
                orderEventMapper.toOrderCreatedEvent(order);

        // Assert
        assertNotNull(event);

        assertEquals(orderId, event.orderId());
        assertEquals(customerId, event.customerId());

        assertEquals(1, event.items().size());

        assertEquals(
                partId,
                event.items().getFirst().partId()
        );

        assertEquals(
                1,
                event.items().getFirst().quantity()
        );
    }

    @Test
    void shouldMapOrderToOrderCreatedEventWithMultipleItems() {

        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        UUID partId1 = UUID.randomUUID();
        UUID partId2 = UUID.randomUUID();

        Order order = Order.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .build();

        OrderItem item1 = OrderItem.builder()
                .partId(partId1)
                .partName("Brake Pad")
                .unitPrice(BigDecimal.valueOf(120.00))
                .quantity(2)
                .subtotal(BigDecimal.valueOf(240.00))
                .build();

        OrderItem item2 = OrderItem.builder()
                .partId(partId2)
                .partName("Oil Filter")
                .unitPrice(BigDecimal.valueOf(50.00))
                .quantity(3)
                .subtotal(BigDecimal.valueOf(150.00))
                .build();

        order.addItem(item1);
        order.addItem(item2);

        // Act
        OrderCreatedEvent event =
                orderEventMapper.toOrderCreatedEvent(order);

        // Assert
        assertNotNull(event);

        assertEquals(orderId, event.orderId());
        assertEquals(customerId, event.customerId());

        assertEquals(2, event.items().size());

        assertEquals(
                partId1,
                event.items().get(0).partId()
        );

        assertEquals(
                2,
                event.items().get(0).quantity()
        );

        assertEquals(
                partId2,
                event.items().get(1).partId()
        );

        assertEquals(
                3,
                event.items().get(1).quantity()
        );
    }
}