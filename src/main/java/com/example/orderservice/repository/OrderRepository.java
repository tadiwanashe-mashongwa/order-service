package com.example.orderservice.repository;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByCustomerId(UUID customerId);

    List<Order> findByStatus(OrderStatus status);

    default List<Order> findAllSorted() {
        return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

}