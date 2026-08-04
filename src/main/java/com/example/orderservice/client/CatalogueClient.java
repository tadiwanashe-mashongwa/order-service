package com.example.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "catalogue-service",
        url = "${catalogue.service.url}"
)
public interface CatalogueClient {

    @GetMapping("/api/parts/{id}")
    PartResponse getPart(@PathVariable UUID id);

}