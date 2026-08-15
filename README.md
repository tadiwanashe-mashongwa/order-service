# SpareLink Order Service

[![CI](https://github.com/tadiwanashe-mashongwa/order-service/actions/workflows/ci.yml/badge.svg)](https://github.com/tadiwanashe-mashongwa/order-service/actions/workflows/ci.yml)
[![JaCoCo coverage](https://github.com/tadiwanashe-mashongwa/order-service/raw/main/.github/badges/jacoco.svg)](https://github.com/tadiwanashe-mashongwa/order-service/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.5.5](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen)](https://spring.io/projects/spring-boot)

Order management microservice for **SpareLink**, an automotive spare-parts platform. It creates customer orders from catalogue prices, persists them safely, publishes lifecycle events through an outbox, and protects APIs with Keycloak JWT roles.

## Highlights

- Java 21, Spring Boot 3.5.5, PostgreSQL, Flyway, JPA/Hibernate
- OpenFeign catalogue integration with mapped 404/503 failures
- Transactional outbox, Kafka publishing, retry backoff, dead-letter and requeue support
- Keycloak JWT security with `CUSTOMER` and `ADMIN` roles
- OpenAPI/Swagger, Actuator, Docker Compose
- Testcontainers for PostgreSQL and Kafka; WireMock for Feign integration

## Architecture

```mermaid
flowchart LR
    Client[Web / Mobile Client] -->|JWT| Order[order-service]
    Keycloak[Keycloak] -->|Issues JWT| Client
    Order -->|Validate JWT| Keycloak
    Order -->|OpenFeign| Catalogue[catalogue-service]
    Order -->|JPA + Flyway| Postgres[(PostgreSQL)]
    Order -->|Outbox relay| Kafka[(Kafka)]
    Kafka --> Inventory[inventory-service]
```

## Order creation flow

```mermaid
sequenceDiagram
    participant C as Client
    participant O as order-service
    participant CA as catalogue-service
    participant DB as PostgreSQL
    participant K as Kafka

    C->>O: POST /api/orders (Bearer JWT)
    O->>CA: Get each part and current price
    CA-->>O: Part details
    O->>DB: Save order, items, and pending outbox event
    O-->>C: 201 Created
    O->>K: Publish order-created
    O->>DB: Mark event published
```

## Database model

```mermaid
erDiagram
    ORDERS ||--o{ ORDER_ITEMS : contains
    ORDERS {
        uuid id PK
        uuid customer_id
        varchar status
        decimal total_amount
        timestamp created_at
        timestamp updated_at
        bigint version
    }
    ORDER_ITEMS {
        uuid id PK
        uuid order_id FK
        uuid part_id
        int quantity
        decimal unit_price
    }
    OUTBOX_EVENTS {
        uuid id PK
        uuid aggregate_id
        varchar topic
        varchar event_type
        boolean published
        int attempt_count
        boolean dead_lettered
        timestamp next_attempt_at
    }
```

## Local run

```bash
docker compose up -d --build
```

Services:

| Service | URL |
|---|---|
| Order API | `http://localhost:8083` |
| Swagger UI | `http://localhost:8083/swagger-ui.html` |
| Health | `http://localhost:8083/actuator/health` |
| Keycloak | `http://localhost:8080` |

The local Keycloak realm is `sparelink`, with development users `customer` / `customer` and `admin` / `admin`. These credentials are strictly for local development.

## API and access control

| Endpoint | CUSTOMER | ADMIN |
|---|---:|---:|
| `POST /api/orders` | Own customer ID only | Yes |
| `GET /api/orders/{id}` | Own order only | Yes |
| `GET /api/orders/customer/{customerId}` | Own customer ID only | Yes |
| `GET /api/orders` | No | Yes |
| `PATCH /api/orders/{id}/status` | No | Yes |
| `/api/outbox/**` | No | Yes |

Health and OpenAPI endpoints are public. Send a Keycloak bearer token for API calls.

## Reliability: transactional outbox

```mermaid
flowchart TD
    A[Order transaction] --> B[Persist order + outbox event]
    B --> C{Relay publishes to Kafka}
    C -->|Success| D[Mark published]
    C -->|Failure| E[Increment attempt count]
    E --> F{Three attempts?}
    F -->|No| G[Retry with exponential delay]
    F -->|Yes| H[Mark dead-lettered]
    H --> I[Admin can inspect or requeue]
```

## Testing strategy

The test suite follows TDD and uses the smallest realistic test layer for each behaviour:

| Layer | Scope |
|---|---|
| Unit | Domain transitions, outbox retry/dead-letter, services, security rules |
| MVC slice | Validation, HTTP problems, pagination, JWT role/ownership access |
| JPA slice | PostgreSQL repository queries and outbox due-event selection |
| Integration | Real Spring context, PostgreSQL/Kafka Testcontainers, Flyway |
| Feign integration | WireMock catalogue responses and mapped failures |

Run everything locally:

```bash
.\mvnw.cmd test
```

The JaCoCo HTML report is generated at `target/site/jacoco/index.html`.

## API screenshots

Swagger UI from the running secured service:

![SpareLink Order Service Swagger UI](docs/images/swagger-ui.png)

## Project structure

```text
src/main/java/com/example/orderservice
├── controller      HTTP API
├── service         application use cases
├── entity          order domain model
├── outbox          reliable event delivery
├── client          catalogue Feign client
├── config          security and OpenAPI configuration
└── exception       ProblemDetail error handling
```
