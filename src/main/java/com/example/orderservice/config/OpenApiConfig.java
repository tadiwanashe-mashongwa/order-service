package com.example.orderservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("SpareLink Order Service API")
                                .description("""
                                        REST API for managing orders in the
                                        SpareLink automotive spare parts platform.
                                        """)
                                .version("v1.0.0")
                                .contact(
                                        new Contact()
                                                .name("SpareLink")
                                                .email("support@sparelink.local")
                                )
                                .license(
                                        new License()
                                                .name("MIT")
                                )
                )
                .externalDocs(
                        new ExternalDocumentation()
                                .description("SpareLink Project")
                                .url("https://github.com/tadiwanashe-mashongwa/order-service")
                );
    }
}