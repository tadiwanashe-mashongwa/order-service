package com.example.orderservice.client;

import com.example.orderservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "catalogue-service",
        url = "${catalogue.service.url}",
        configuration = FeignConfig.class
)
public interface CatalogueClient {

    @GetMapping("/api/parts/{id}")
    ApiResponse<PartResponse> getPartById(@PathVariable UUID id);

}