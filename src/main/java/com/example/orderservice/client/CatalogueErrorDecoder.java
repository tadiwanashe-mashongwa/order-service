package com.example.orderservice.client;

import com.example.orderservice.exception.CatalogueUnavailableException;
import com.example.orderservice.exception.PartNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class CatalogueErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    @Override
    public Exception decode(String methodKey, Response response) {

        try {

            ApiResponse<?> error =
                    objectMapper.readValue(
                            response.body().asInputStream(),
                            ApiResponse.class
                    );

            log.warn(
                    "Catalogue service returned {} for {} : {}",
                    response.status(),
                    methodKey,
                    error.message()
            );

            return switch (response.status()) {

                case 404 ->
                        new PartNotFoundException(error.message());

                case 503 ->
                        new CatalogueUnavailableException(error.message());

                default ->
                        new ErrorDecoder.Default()
                                .decode(methodKey, response);
            };

        } catch (IOException e) {

            log.error("Failed to decode catalogue error response", e);

            return new ErrorDecoder.Default()
                    .decode(methodKey, response);
        }
    }
}