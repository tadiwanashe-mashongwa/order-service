package com.example.orderservice.repository;

import com.example.orderservice.config.AbstractPostgresContainerTest;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class OrderRepositoryTest extends AbstractPostgresContainerTest {

    @Autowired
    private OrderRepository orderRepository;
    private Order createOrder(
            UUID customerId,
            OrderStatus status,
            BigDecimal totalAmount
    ) {
        return Order.builder()
                .customerId(customerId)
                .status(status)
                .totalAmount(totalAmount)
                .build();
    }

    @Test
    void shouldFindOrdersByStatus() {

        // Given
        orderRepository.save(
                createOrder(
                        UUID.randomUUID(),
                        OrderStatus.PENDING,
                        new BigDecimal("100.00")
                )
        );

        orderRepository.save(
                createOrder(
                        UUID.randomUUID(),
                        OrderStatus.PENDING,
                        new BigDecimal("250.00")
                )
        );

        orderRepository.save(
                createOrder(
                        UUID.randomUUID(),
                        OrderStatus.PAID,
                        new BigDecimal("500.00")
                )
        );

        // When
        Page<Order> result =
                orderRepository.findByStatus(
                        OrderStatus.PENDING,
                        PageRequest.of(0, 10)
                );

        // Then
        assertEquals(2, result.getTotalElements());

        assertTrue(
                result.getContent()
                        .stream()
                        .allMatch(order ->
                                order.getStatus() == OrderStatus.PENDING)
        );
    }

    @Test
    void shouldReturnEmptyPageWhenStatusDoesNotExist() {

        // Given
        orderRepository.save(
                createOrder(
                        UUID.randomUUID(),
                        OrderStatus.PAID,
                        new BigDecimal("200.00")
                )
        );

        // When
        Page<Order> result =
                orderRepository.findByStatus(
                        OrderStatus.CANCELLED,
                        PageRequest.of(0, 10)
                );

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindOrdersByCustomerId() {

        // Given
        UUID customerId = UUID.randomUUID();

        orderRepository.save(
                createOrder(
                        customerId,
                        OrderStatus.PENDING,
                        new BigDecimal("150.00")
                )
        );

        orderRepository.save(
                createOrder(
                        customerId,
                        OrderStatus.PAID,
                        new BigDecimal("350.00")
                )
        );

        orderRepository.save(
                createOrder(
                        UUID.randomUUID(),
                        OrderStatus.PENDING,
                        new BigDecimal("500.00")
                )
        );

        // When
        Page<Order> result =
                orderRepository.findByCustomerId(
                        customerId,
                        PageRequest.of(0, 10)
                );

        // Then
        assertEquals(2, result.getTotalElements());

        assertTrue(
                result.getContent()
                        .stream()
                        .allMatch(order ->
                                order.getCustomerId().equals(customerId))
        );
    }

    @Test
    void shouldReturnEmptyPageWhenCustomerHasNoOrders() {

        // Given
        orderRepository.save(
                createOrder(
                        UUID.randomUUID(),
                        OrderStatus.PENDING,
                        new BigDecimal("250.00")
                )
        );

        // When
        Page<Order> result =
                orderRepository.findByCustomerId(
                        UUID.randomUUID(),
                        PageRequest.of(0, 10)
                );

        // Then
        assertTrue(result.isEmpty());
    }
}