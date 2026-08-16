package com.example.orderservice.consumer;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.event.StockReservedEvent;
import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component
public class StockReservedEventListener {
 private final OrderService service; private final ObjectMapper mapper;
 public StockReservedEventListener(OrderService service,ObjectMapper mapper){this.service=service;this.mapper=mapper;}
 @KafkaListener(topics="stock-reserved",groupId="order-service")
 public void handle(String payload){try{var e=mapper.readValue(payload,StockReservedEvent.class);service.transitionOrderStatus(e.orderId(),OrderStatus.STOCK_RESERVED);service.transitionOrderStatus(e.orderId(),OrderStatus.PAYMENT_PENDING);}catch(Exception e){throw new IllegalArgumentException("Invalid stock-reserved event",e);}}
}
