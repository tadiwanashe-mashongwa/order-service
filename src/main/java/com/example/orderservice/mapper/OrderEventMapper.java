package com.example.orderservice.mapper;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.event.OrderItemEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderEventMapper {

    public OrderCreatedEvent toOrderCreatedEvent(Order order) {

        List<OrderItemEvent> items = order.getItems()
                .stream()
                .map(this::toOrderItemEvent)
                .toList();

        return new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                items
        );
    }

    private OrderItemEvent toOrderItemEvent(OrderItem item) {

        return new OrderItemEvent(
                item.getPartId(),
                item.getQuantity()
        );
    }

}
