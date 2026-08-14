package com.example.orderservice.config;

import com.example.orderservice.controller.OrderController;
import com.example.orderservice.dto.CreateOrderItemRequest;
import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.List;
import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void shouldRejectUnauthenticatedOrderRequests() throws Exception {

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectCustomerStatusTransition() throws Exception {

        mockMvc.perform(patch("/api/orders/{orderId}/status", UUID.randomUUID())
                        .param("status", "STOCK_RESERVED")
                        .with(jwt().authorities(
                                createAuthorityList("ROLE_CUSTOMER")
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminStatusTransition() throws Exception {

        mockMvc.perform(patch("/api/orders/{orderId}/status", UUID.randomUUID())
                        .param("status", "STOCK_RESERVED")
                        .with(jwt().authorities(
                                createAuthorityList("ROLE_ADMIN")
                        )))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectCustomerCreatingOrderForAnotherCustomer() throws Exception {

        CreateOrderRequest request = new CreateOrderRequest(
                UUID.randomUUID(),
                List.of(new CreateOrderItemRequest(UUID.randomUUID(), 1))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content("{\"customerId\":\"" + request.customerId()
                                + "\",\"items\":[{\"partId\":\""
                                + request.items().getFirst().partId()
                                + "\",\"quantity\":1}]}")
                        .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))
                                .authorities(createAuthorityList("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectCustomerReadingAllOrders() throws Exception {

        mockMvc.perform(get("/api/orders")
                        .with(jwt().authorities(
                                createAuthorityList("ROLE_CUSTOMER")
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectCustomerReadingAnotherCustomersOrders() throws Exception {

        mockMvc.perform(get("/api/orders/customer/{customerId}", UUID.randomUUID())
                        .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))
                                .authorities(createAuthorityList("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectCustomerReadingAnotherCustomersOrder() throws Exception {

        UUID orderId = UUID.randomUUID();
        when(orderService.getOrderById(orderId)).thenReturn(new OrderResponse(
                orderId, UUID.randomUUID(), OrderStatus.PENDING,
                BigDecimal.TEN, List.of(), Instant.now()
        ));

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))
                                .authorities(createAuthorityList("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }
}
