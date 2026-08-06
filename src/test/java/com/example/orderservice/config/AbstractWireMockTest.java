package com.example.orderservice.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public abstract class AbstractWireMockTest
        extends AbstractPostgresContainerTest {

    protected static final WireMockServer wireMockServer =
            new WireMockServer(
                    wireMockConfig().dynamicPort()
            );

    @BeforeAll
    static void startWireMock() {

        wireMockServer.start();

    }

    @AfterAll
    static void stopWireMock() {

        wireMockServer.stop();

    }

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry
    ) {

        registry.add(
                "catalogue.service.url",
                wireMockServer::baseUrl
        );

    }

}