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

    @Test
    void shouldFindOrdersByCustomerId() {

        // Given
        UUID customerId = UUID.randomUUID();

        Order order = Order.builder()
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("250.00"))
                .build();

        orderRepository.save(order);

        // When
        Page<Order> result =
                orderRepository.findByCustomerId(
                        customerId,
                        PageRequest.of(0, 10)
                );

        // Then
        assertEquals(1, result.getTotalElements());

        Order found = result.getContent().getFirst();

        assertEquals(customerId, found.getCustomerId());
        assertEquals(OrderStatus.PENDING, found.getStatus());
        assertEquals(
                new BigDecimal("250.00"),
                found.getTotalAmount()
        );
    }

    @Test
    void shouldReturnEmptyPageWhenCustomerHasNoOrders() {

        // Given
        UUID customerId = UUID.randomUUID();

        // When
        Page<Order> result =
                orderRepository.findByCustomerId(
                        customerId,
                        PageRequest.of(0, 10)
                );

        // Then
        assertTrue(result.isEmpty());
    }

}