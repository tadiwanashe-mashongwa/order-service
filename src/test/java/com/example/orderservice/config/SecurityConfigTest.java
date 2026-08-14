package com.example.orderservice.config;

import com.example.orderservice.controller.OrderController;
import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
}
