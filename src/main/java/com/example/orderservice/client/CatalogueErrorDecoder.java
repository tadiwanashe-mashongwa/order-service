package com.example.orderservice.client;

import com.example.orderservice.exception.CatalogueUnavailableException;
import com.example.orderservice.exception.PartNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

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

            return new ErrorDecoder.Default()
                    .decode(methodKey, response);

        }
    }
}