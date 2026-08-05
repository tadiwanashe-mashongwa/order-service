package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.CreateOrderResponse;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.OrderSummaryResponse;
import com.example.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateOrderResponse createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.createOrder(request);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrderById(
            @PathVariable UUID orderId
    ) {
        return orderService.getOrderById(orderId);
    }

    @GetMapping
    public List<OrderSummaryResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/customer/{customerId}")
    public List<OrderSummaryResponse> getOrdersByCustomer(
            @PathVariable UUID customerId
    ) {
        return orderService.getOrdersByCustomer(customerId);
    }
}