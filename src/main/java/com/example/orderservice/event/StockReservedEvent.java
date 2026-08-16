package com.example.orderservice.event;
import java.util.UUID;
public record StockReservedEvent(UUID orderId) { }
