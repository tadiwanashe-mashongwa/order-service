package com.example.orderservice.config;

import com.example.orderservice.client.CatalogueErrorDecoder;
import com.example.orderservice.observability.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder(ObjectMapper objectMapper) {
        return new CatalogueErrorDecoder(objectMapper);
    }

    @Bean
    public RequestInterceptor correlationIdRequestInterceptor() {
        return requestTemplate -> {
            String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (correlationId != null) {
                requestTemplate.header(CorrelationIdFilter.HEADER_NAME, correlationId);
            }
        };
    }

}
