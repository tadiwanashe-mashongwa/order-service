package com.example.orderservice.consumer;

import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentStatusChangedEventListenerTest {

    @Mock
    private OrderService orderService;

    @Test
    void shouldMarkPaymentPendingOrderAsPaidWhenPaymentSucceeds() {
        UUID orderId = UUID.randomUUID();
        PaymentStatusChangedEventListener listener = new PaymentStatusChangedEventListener(orderService, new ObjectMapper());

        listener.handlePaymentStatusChanged("""
                {
                  "paymentId": "%s",
                  "orderId": "%s",
                  "status": "SUCCESS"
                }
                """.formatted(UUID.randomUUID(), orderId));

        verify(orderService).transitionOrderStatus(orderId, OrderStatus.PAID);
    }

    @Test
    void shouldMarkPaymentPendingOrderAsPaymentFailedWhenPaymentFails() {
        UUID orderId = UUID.randomUUID();
        PaymentStatusChangedEventListener listener = new PaymentStatusChangedEventListener(orderService, new ObjectMapper());

        listener.handlePaymentStatusChanged("""
                {
                  "paymentId": "%s",
                  "orderId": "%s",
                  "status": "FAILED"
                }
                """.formatted(UUID.randomUUID(), orderId));

        verify(orderService).transitionOrderStatus(orderId, OrderStatus.PAYMENT_FAILED);
    }

    @Test
    void shouldIgnoreNonTerminalPaymentStatus() {
        PaymentStatusChangedEventListener listener = new PaymentStatusChangedEventListener(orderService, new ObjectMapper());

        listener.handlePaymentStatusChanged("""
                {
                  "paymentId": "%s",
                  "orderId": "%s",
                  "status": "PENDING"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID()));

        verifyNoMoreInteractions(orderService);
    }
}
