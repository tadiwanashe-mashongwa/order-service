package com.example.orderservice.config;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Configuration(proxyBeanMethods = false)
public class KafkaTestContainer extends AbstractPostgresContainerTest {

    @ServiceConnection
    static KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse(
                            "apache/kafka-native:3.8.0"
                    )
            );

    static {
        kafka.start();
    }

    @DynamicPropertySource
    static void configureKafka(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.kafka.bootstrap-servers",
                kafka::getBootstrapServers
        );
    }

}
