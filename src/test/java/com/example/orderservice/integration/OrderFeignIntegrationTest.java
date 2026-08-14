package com.example.orderservice.integration;

import com.example.orderservice.config.AbstractWireMockTest;
import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.CreateOrderResponse;
import com.example.orderservice.exception.CatalogueUnavailableException;
import com.example.orderservice.exception.PartNotFoundException;
import com.example.orderservice.producer.OrderEventProducer;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.support.TestDataFactory;
import com.example.orderservice.support.WireMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Transactional
class OrderFeignIntegrationTest extends AbstractWireMockTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private OrderEventProducer orderEventProducer;

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
    }
    @Test
    void shouldCreateOrderSuccessfullyUsingFeign() {

        // Given
        UUID partId = UUID.randomUUID();

        WireMockSupport.stubPart(
                wireMockServer,
                partId,
                "Brake Pads",
                250
        );

        CreateOrderRequest request =
                TestDataFactory.singleItemOrderRequest(partId);

        // When
        CreateOrderResponse response =
                orderService.createOrder(request);

        // Then
        assertNotNull(response.orderId());

        assertEquals(
                request.customerId(),
                response.customerId()
        );

        assertEquals(
                new BigDecimal("250"),
                response.totalAmount()
        );

        assertEquals(
                1,
                orderRepository.count()
        );

        WireMockSupport.verifyPartRequest(
                wireMockServer,
                partId
        );

        verifyNoInteractions(orderEventProducer);
    }
    @Test
    void shouldThrowPartNotFoundWhenFeignReceives404() {

        // Given
        UUID partId = UUID.randomUUID();

        WireMockSupport.stubPartNotFound(
                wireMockServer,
                partId
        );

        CreateOrderRequest request =
                TestDataFactory.singleItemOrderRequest(partId);

        // When + Then
        assertThrows(
                PartNotFoundException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals(
                0,
                orderRepository.count()
        );

        verify(orderEventProducer, never())
                .publishOrderCreated(any());

        WireMockSupport.verifyPartRequest(
                wireMockServer,
                partId
        );
    }
    @Test
    void shouldThrowCatalogueUnavailableWhenFeignReceives503() {

        // Given
        UUID partId = UUID.randomUUID();

        WireMockSupport.stubCatalogueUnavailable(
                wireMockServer,
                partId
        );

        CreateOrderRequest request =
                TestDataFactory.singleItemOrderRequest(partId);

        // When + Then
        assertThrows(
                CatalogueUnavailableException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals(
                0,
                orderRepository.count()
        );

        verify(orderEventProducer, never())
                .publishOrderCreated(any());

        WireMockSupport.verifyPartRequest(
                wireMockServer,
                partId
        );
    }
    @Test
    void shouldCreateOrderWithMultipleItemsUsingFeign() {

        // Given
        UUID part1 = UUID.randomUUID();
        UUID part2 = UUID.randomUUID();

        WireMockSupport.stubPart(
                wireMockServer,
                part1,
                "Brake Pads",
                120
        );

        WireMockSupport.stubPart(
                wireMockServer,
                part2,
                "Oil Filter",
                80
        );

        CreateOrderRequest request =
                TestDataFactory.multiItemOrderRequest(part1, part2);

        // When
        CreateOrderResponse response =
                orderService.createOrder(request);

        // Then
        assertNotNull(response.orderId());

        assertEquals(
                new BigDecimal("280"),
                response.totalAmount()
        );

        assertEquals(
                1,
                orderRepository.count()
        );

        WireMockSupport.verifyPartRequest(
                wireMockServer,
                part1
        );

        WireMockSupport.verifyPartRequest(
                wireMockServer,
                part2
        );

        verifyNoInteractions(orderEventProducer);
    }
    @Test
    void shouldCallCatalogueOncePerRequestedPart() {

        // Given
        UUID part1 = UUID.randomUUID();
        UUID part2 = UUID.randomUUID();

        WireMockSupport.stubPart(
                wireMockServer,
                part1,
                "Brake Pads",
                100
        );

        WireMockSupport.stubPart(
                wireMockServer,
                part2,
                "Oil Filter",
                50
        );

        CreateOrderRequest request =
                TestDataFactory.multiItemOrderRequest(part1, part2);

        // When
        orderService.createOrder(request);

        // Then
        WireMockSupport.verifyPartRequest(
                wireMockServer,
                part1
        );

        WireMockSupport.verifyPartRequest(
                wireMockServer,
                part2
        );

        assertEquals(
                1,
                orderRepository.count()
        );

        verifyNoInteractions(orderEventProducer);
    }
}
