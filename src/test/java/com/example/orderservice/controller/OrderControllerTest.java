package com.example.orderservice.controller;

import com.example.orderservice.dto.*;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.exception.InvalidOrderStatusTransitionException;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import com.example.orderservice.config.SecurityConfig;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void shouldCreateOrder() throws Exception {

        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();

        CreateOrderRequest request =
                new CreateOrderRequest(
                        customerId,
                        List.of(
                                new CreateOrderItemRequest(
                                        partId,
                                        2
                                )
                        )
                );

        CreateOrderResponse response =
                new CreateOrderResponse(
                        orderId,
                        customerId,
                        OrderStatus.PENDING,
                        new BigDecimal("240"),
                        Instant.now()
                );

        when(orderService.createOrder(any()))
                .thenReturn(response);

        // When + Then
        mockMvc.perform(
                        post("/api/orders")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(240));

        verify(orderService).createOrder(any());
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid() throws Exception {

        // Given
        String invalidRequest = """
                {
                  "customerId": null,
                  "items": []
                }
                """;

        // When + Then
        mockMvc.perform(
                        post("/api/orders")
                                .contentType(APPLICATION_JSON)
                                .content(invalidRequest)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldReturnBadRequestWhenOrderRequestJsonIsMalformed() throws Exception {

        String malformedRequest = """
                {
                  "customerId": "not-a-uuid",
                  "items": []
                }
                """;

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(APPLICATION_JSON)
                                .content(malformedRequest)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title")
                        .value("Malformed Request"))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(orderService);
    }
    @Test
    void shouldReturnOrderWhenOrderExists() throws Exception {

        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();

        OrderResponse response =
                new OrderResponse(
                        orderId,
                        customerId,
                        OrderStatus.PENDING,
                        new BigDecimal("240"),
                        List.of(),
                        Instant.now()
                );

        when(orderService.getOrderById(orderId))
                .thenReturn(response);

        // When + Then
        mockMvc.perform(
                        get("/api/orders/{orderId}", orderId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(240));

        verify(orderService).getOrderById(orderId);
    }
    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {

        // Given
        UUID orderId = UUID.randomUUID();

        when(orderService.getOrderById(orderId))
                .thenThrow(
                        new OrderNotFoundException(
                                "Order not found"
                        )
                );

        // When + Then
        mockMvc.perform(
                        get("/api/orders/{orderId}", orderId)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Order Not Found"))
                .andExpect(jsonPath("$.detail").value("Order not found"))
                .andExpect(jsonPath("$.status").value(404));

        verify(orderService).getOrderById(orderId);
    }
    @Test
    void shouldReturnAllOrders() throws Exception {

        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        OrderSummaryResponse response =
                new OrderSummaryResponse(
                        orderId,
                        customerId,
                        OrderStatus.PENDING,
                        new BigDecimal("240"),
                        Instant.now()
                );

        Page<OrderSummaryResponse> page =
                new PageImpl<>(List.of(response));

        when(orderService.getAllOrders(
                isNull(),
                any(Pageable.class)))
                .thenReturn(page);

        // When + Then
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId")
                        .value(orderId.toString()))
                .andExpect(jsonPath("$.content[0].status")
                        .value("PENDING"))
                .andExpect(jsonPath("$.totalElements")
                        .value(1));

        verify(orderService)
                .getAllOrders(
                        isNull(),
                        any(Pageable.class)
                );
    }
    @Test
    void shouldReturnOrdersFilteredByStatus() throws Exception {

        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        OrderSummaryResponse response =
                new OrderSummaryResponse(
                        orderId,
                        customerId,
                        OrderStatus.PENDING,
                        new BigDecimal("240"),
                        Instant.now()
                );

        Page<OrderSummaryResponse> page =
                new PageImpl<>(List.of(response));

        when(orderService.getAllOrders(
                eq(OrderStatus.PENDING),
                any(Pageable.class)))
                .thenReturn(page);

        // When + Then
        mockMvc.perform(
                        get("/api/orders")
                                .param("status", "PENDING")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status")
                        .value("PENDING"))
                .andExpect(jsonPath("$.totalElements")
                        .value(1));

        verify(orderService)
                .getAllOrders(
                        eq(OrderStatus.PENDING),
                        any(Pageable.class)
                );
    }
    @Test
    void shouldReturnOrdersForCustomer() throws Exception {

        // Given
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        OrderSummaryResponse response =
                new OrderSummaryResponse(
                        orderId,
                        customerId,
                        OrderStatus.PENDING,
                        new BigDecimal("240"),
                        Instant.now()
                );

        Page<OrderSummaryResponse> page =
                new PageImpl<>(List.of(response));

        when(orderService.getOrdersByCustomer(
                eq(customerId),
                any(Pageable.class)))
                .thenReturn(page);

        // When + Then
        mockMvc.perform(
                        get("/api/orders/customer/{customerId}", customerId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerId")
                        .value(customerId.toString()))
                .andExpect(jsonPath("$.content[0].status")
                        .value("PENDING"))
                .andExpect(jsonPath("$.totalElements")
                        .value(1));

        verify(orderService)
                .getOrdersByCustomer(
                        eq(customerId),
                        any(Pageable.class)
                );
    }
    @Test
    void shouldReturnEmptyPageWhenCustomerHasNoOrders() throws Exception {

        // Given
        UUID customerId = UUID.randomUUID();

        Page<OrderSummaryResponse> emptyPage =
                Page.empty();

        when(orderService.getOrdersByCustomer(
                eq(customerId),
                any(Pageable.class)))
                .thenReturn(emptyPage);

        // When + Then
        mockMvc.perform(
                        get("/api/orders/customer/{customerId}", customerId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(orderService)
                .getOrdersByCustomer(
                        eq(customerId),
                        any(Pageable.class)
                );
    }

    @Test
    void shouldTransitionOrderStatus() throws Exception {

        UUID orderId = UUID.randomUUID();

        mockMvc.perform(
                        patch("/api/orders/{orderId}/status", orderId)
                                .param("status", "STOCK_RESERVED")
                )
                .andExpect(status().isNoContent());

        verify(orderService).transitionOrderStatus(
                orderId,
                OrderStatus.STOCK_RESERVED
        );
    }

    @Test
    void shouldReturnConflictForInvalidOrderStatusTransition() throws Exception {

        UUID orderId = UUID.randomUUID();

        org.mockito.Mockito.doThrow(
                        new InvalidOrderStatusTransitionException(
                                "Cannot transition from DELIVERED to PENDING"
                        )
                )
                .when(orderService)
                .transitionOrderStatus(orderId, OrderStatus.PENDING);

        mockMvc.perform(
                        patch("/api/orders/{orderId}/status", orderId)
                                .param("status", "PENDING")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title")
                        .value("Invalid Order Status Transition"))
                .andExpect(jsonPath("$.status").value(409));

        verify(orderService).transitionOrderStatus(
                orderId,
                OrderStatus.PENDING
        );
    }

    @Test
    void shouldReturnNotFoundWhenTransitioningAnUnknownOrder() throws Exception {

        UUID orderId = UUID.randomUUID();

        org.mockito.Mockito.doThrow(
                        new OrderNotFoundException("Order not found")
                )
                .when(orderService)
                .transitionOrderStatus(orderId, OrderStatus.STOCK_RESERVED);

        mockMvc.perform(
                        patch("/api/orders/{orderId}/status", orderId)
                                .param("status", "STOCK_RESERVED")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Order Not Found"))
                .andExpect(jsonPath("$.status").value(404));

        verify(orderService).transitionOrderStatus(
                orderId,
                OrderStatus.STOCK_RESERVED
        );
    }

    @Test
    void shouldReturnBadRequestForUnknownOrderStatus() throws Exception {

        mockMvc.perform(
                        patch("/api/orders/{orderId}/status", UUID.randomUUID())
                                .param("status", "UNKNOWN")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(orderService);
    }
}
