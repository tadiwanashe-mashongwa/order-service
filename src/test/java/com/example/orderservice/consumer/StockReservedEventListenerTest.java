package com.example.orderservice.consumer;

import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.mockito.Mockito.*;

class StockReservedEventListenerTest {
 @Test void advancesReservedOrderToPaymentPending() {
  OrderService service=mock(OrderService.class); UUID orderId=UUID.randomUUID();
  new StockReservedEventListener(service,new ObjectMapper()).handle("{\"orderId\":\""+orderId+"\"}");
  var order=inOrder(service);
  order.verify(service).transitionOrderStatus(orderId, OrderStatus.STOCK_RESERVED);
  order.verify(service).transitionOrderStatus(orderId, OrderStatus.PAYMENT_PENDING);
 }
}
