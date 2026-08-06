package com.example.orderservice.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostgresContainerTest extends AbstractPostgresContainerTest {

    @Test
    void shouldStartPostgresContainer() {

        assertTrue(postgres.isRunning());

        assertNotNull(postgres.getJdbcUrl());

        assertNotNull(postgres.getUsername());

        assertNotNull(postgres.getPassword());

    }

}