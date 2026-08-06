package com.example.orderservice.support;

import com.example.orderservice.dto.CreateOrderItemRequest;
import com.example.orderservice.dto.CreateOrderRequest;

import java.util.List;
import java.util.UUID;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    /* ==========================================================
       Order Requests
       ========================================================== */

    public static CreateOrderRequest singleItemOrderRequest(
            UUID partId
    ) {

        return new CreateOrderRequest(
                UUID.randomUUID(),
                List.of(
                        new CreateOrderItemRequest(
                                partId,
                                1
                        )
                )
        );
    }

    public static CreateOrderRequest multipleItemOrderRequest(
            UUID brakePadsId,
            UUID oilFilterId
    ) {

        return new CreateOrderRequest(
                UUID.randomUUID(),
                List.of(
                        new CreateOrderItemRequest(
                                brakePadsId,
                                2
                        ),
                        new CreateOrderItemRequest(
                                oilFilterId,
                                3
                        )
                )
        );
    }

    /* ==========================================================
       WireMock JSON Responses
       ========================================================== */

    public static String partResponse(
            UUID partId,
            String partName,
            long amount
    ) {

        return """
                {
                  "success": true,
                  "message": "Success",
                  "data": {
                    "id": "%s",
                    "name": "%s",
                    "price": {
                      "amount": %d,
                      "currency": "USD"
                    }
                  },
                  "timestamp": "2026-08-06T12:00:00Z"
                }
                """.formatted(
                partId,
                partName,
                amount
        );
    }

    public static String partNotFound() {

        return """
                {
                  "success": false,
                  "message": "Part not found",
                  "data": null,
                  "timestamp": "2026-08-06T12:00:00Z"
                }
                """;
    }

    public static String catalogueUnavailable() {

        return """
                {
                  "success": false,
                  "message": "Catalogue unavailable",
                  "data": null,
                  "timestamp": "2026-08-06T12:00:00Z"
                }
                """;
    }

}