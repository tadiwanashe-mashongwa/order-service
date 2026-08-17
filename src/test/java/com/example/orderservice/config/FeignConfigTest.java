package com.example.orderservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class FeignConfigTest {

    @Test
    void shouldPropagateCorrelationIdToCatalogueRequests() {
        MDC.put("correlationId", "order-flow-456");
        try {
            RequestInterceptor interceptor = new FeignConfig().correlationIdRequestInterceptor();
            RequestTemplate template = new RequestTemplate();

            interceptor.apply(template);

            assertThat(template.headers().get("X-Correlation-Id")).containsExactly("order-flow-456");
        } finally {
            MDC.clear();
        }
    }
}
