package com.example.orderservice.support;

import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public final class WireMockSupport {

    private WireMockSupport() {
    }

    public static void stubPart(
            WireMockServer wireMockServer,
            UUID partId,
            String partName,
            long amount
    ) {

        wireMockServer.stubFor(
                get(urlEqualTo("/api/parts/" + partId))
                        .willReturn(
                                okJson(
                                        TestDataFactory.partResponse(
                                                partId,
                                                partName,
                                                amount
                                        )
                                )
                        )
        );
    }

    public static void stubPartNotFound(
            WireMockServer wireMockServer,
            UUID partId
    ) {

        wireMockServer.stubFor(
                get(urlEqualTo("/api/parts/" + partId))
                        .willReturn(
                                aResponse()
                                        .withStatus(404)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody(
                                                TestDataFactory.partNotFound()
                                        )
                        )
        );
    }

    public static void stubCatalogueUnavailable(
            WireMockServer wireMockServer,
            UUID partId
    ) {

        wireMockServer.stubFor(
                get(urlEqualTo("/api/parts/" + partId))
                        .willReturn(
                                aResponse()
                                        .withStatus(503)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody(
                                                TestDataFactory.catalogueUnavailable()
                                        )
                        )
        );
    }

    public static void verifyPartRequest(
            WireMockServer wireMockServer,
            UUID partId
    ) {

        wireMockServer.verify(
                1,
                getRequestedFor(
                        urlEqualTo("/api/parts/" + partId)
                )
        );
    }

    public static void verifyPartRequests(
            WireMockServer wireMockServer,
            UUID... partIds
    ) {

        for (UUID partId : partIds) {

            verifyPartRequest(
                    wireMockServer,
                    partId
            );
        }
    }

}