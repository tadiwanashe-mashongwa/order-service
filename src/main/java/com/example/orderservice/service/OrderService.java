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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
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

        log.info("Creating order for customer {}", request.customerId());

        Order order = Order.builder()
                .customerId(request.customerId())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest : request.items()) {

            log.info("Fetching part {} from catalogue", itemRequest.partId());

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

        log.info("Order {} created successfully", savedOrder.getId());

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

        log.info("Fetching order {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order {} not found", orderId);
                    return new OrderNotFoundException(
                            "Order not found with id: " + orderId);
                });

        return orderMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAllOrders(
            OrderStatus status,
            Pageable pageable
    ) {

        log.info("Fetching orders. Status filter={}", status);

        Page<Order> orders = (status == null)
                ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable);

        return orders.map(orderMapper::toOrderSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getOrdersByCustomer(
            UUID customerId,
            Pageable pageable
    ) {

        log.info("Fetching orders for customer {}", customerId);

        return orderRepository.findByCustomerId(customerId, pageable)
                .map(orderMapper::toOrderSummaryResponse);
    }

    @Transactional
    public void transitionOrderStatus(
            UUID orderId,
            OrderStatus targetStatus
    ) {

        log.info("Transitioning order {} to {}", orderId, targetStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order {} not found", orderId);
                    return new OrderNotFoundException(
                            "Order not found with id: " + orderId);
                });

        order.transitionTo(targetStatus);

        log.info("Order {} transitioned to {}", orderId, targetStatus);
    }
}