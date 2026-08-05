package com.example.orderservice.mapper;

import com.example.orderservice.dto.OrderItemResponse;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.OrderSummaryResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toOrderResponse(Order order) {

        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(this::toOrderItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt()
        );
    }

    public OrderSummaryResponse toOrderSummaryResponse(Order order) {

        return new OrderSummaryResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {

        return new OrderItemResponse(
                item.getPartId(),
                item.getPartName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }
}