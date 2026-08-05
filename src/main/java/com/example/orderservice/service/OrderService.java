package com.example.orderservice.service;

import com.example.orderservice.client.ApiResponse;
import com.example.orderservice.client.CatalogueClient;
import com.example.orderservice.client.PartResponse;
import com.example.orderservice.dto.CreateOrderItemRequest;
import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.CreateOrderResponse;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.OrderSummaryResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.mapper.OrderEventMapper;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.producer.OrderEventProducer;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CatalogueClient catalogueClient;
    private final OrderEventMapper orderEventMapper;
    private final OrderMapper orderMapper;
    private final OrderEventProducer orderEventProducer;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {

        Order order = Order.builder()
                .customerId(request.customerId())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest : request.items()) {

            ApiResponse<PartResponse> response =
                    catalogueClient.getPartById(itemRequest.partId());

            PartResponse part = response.data();

            BigDecimal unitPrice =
                    BigDecimal.valueOf(part.price().amount());

            BigDecimal subtotal =
                    unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantity()));

            OrderItem orderItem = OrderItem.builder()
                    .partId(itemRequest.partId())
                    .partName(part.name())
                    .unitPrice(unitPrice)
                    .quantity(itemRequest.quantity())
                    .subtotal(subtotal)
                    .build();

            order.addItem(orderItem);

            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        OrderCreatedEvent event =
                orderEventMapper.toOrderCreatedEvent(savedOrder);

        orderEventProducer.publishOrderCreated(event);

        return new CreateOrderResponse(
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                savedOrder.getStatus(),
                savedOrder.getTotalAmount(),
                savedOrder.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException(
                                "Order not found with id: " + orderId));

        return orderMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getAllOrders() {

        return orderRepository.findAllSorted()
                .stream()
                .map(orderMapper::toOrderSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getOrdersByCustomer(UUID customerId) {

        return orderRepository.findByCustomerId(customerId)
                .stream()
                .map(orderMapper::toOrderSummaryResponse)
                .toList();
    }
}