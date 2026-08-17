package com.example.orderservice.observability;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CorrelationIdFilterTest {

    @Test
    void shouldUseIncomingCorrelationIdForTheRequestAndResponse() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "order-flow-123");
        var response = new MockHttpServletResponse();
        var observedCorrelationId = new AtomicReference<String>();

        new CorrelationIdFilter().doFilter(request, response,
                (ignoredRequest, ignoredResponse) -> observedCorrelationId.set(MDC.get("correlationId")));

        assertThat(observedCorrelationId.get()).isEqualTo("order-flow-123");
        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("order-flow-123");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void shouldGenerateCorrelationIdWhenTheRequestDoesNotContainOne() throws Exception {
        var response = new MockHttpServletResponse();

        new CorrelationIdFilter().doFilter(new MockHttpServletRequest(), response,
                (ignoredRequest, ignoredResponse) -> { });

        assertThatCode(() -> UUID.fromString(response.getHeader("X-Correlation-Id"))).doesNotThrowAnyException();
    }
}
