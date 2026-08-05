package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.CreateOrderResponse;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.OrderSummaryResponse;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(
        name = "Orders",
        description = "Operations for creating and retrieving customer orders."
)
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new order",
            description = "Creates a new customer order and publishes an OrderCreated event."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public CreateOrderResponse createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.createOrder(request);
    }

    @GetMapping("/{orderId}")
    @Operation(
            summary = "Get order by ID",
            description = "Returns a complete order including all order items."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema())
            )
    })
    public OrderResponse getOrderById(

            @Parameter(description = "Order ID")
            @PathVariable UUID orderId

    ) {

        return orderService.getOrderById(orderId);

    }

    @GetMapping
    @Operation(
            summary = "Get all orders",
            description = "Returns paginated orders with an optional status filter."
    )
    public Page<OrderSummaryResponse> getAllOrders(

            @Parameter(
                    description = "Optional order status filter"
            )
            @RequestParam(required = false)
            OrderStatus status,

            @ParameterObject
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable

    ) {

        return orderService.getAllOrders(
                status,
                pageable
        );

    }

    @GetMapping("/customer/{customerId}")
    @Operation(
            summary = "Get customer orders",
            description = "Returns paginated orders for a specific customer."
    )
    public Page<OrderSummaryResponse> getOrdersByCustomer(

            @Parameter(description = "Customer ID")
            @PathVariable UUID customerId,

            @ParameterObject
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable

    ) {

        return orderService.getOrdersByCustomer(
                customerId,
                pageable
        );

    }

}