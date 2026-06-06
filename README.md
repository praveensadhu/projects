<!-- @author "Venkata Praveen Kumar Gupta" -->
# Event Ledger System

A robust, microservices-based financial transaction ledger designed to handle out-of-order event delivery and guarantee transaction idempotency.

## Architecture Overview

The system is composed of two decoupled Spring Boot microservices that interact to maintain a consistent financial state:

- **Event Gateway API (Port 8080)**: The public-facing entry point. It performs schema validation, enforces idempotency using a local H2 database, and manages the lifecycle of incoming transaction events.
- **Account Service (Port 8081)**: An internal service responsible for the core business logic of applying credits/debits, maintaining account balances, and tracking transaction history.

### Interaction Flow
1. Clients submit events to the Gateway via `POST /events`.
2. Gateway persists the event as `PENDING` and performs a synchronous REST call to the Account Service.
3. Distributed tracing is maintained across the call via the `X-Trace-Id` header.
4. Upon successful processing by the Account Service, the Gateway updates the event status to `SUBMITTED`.

> [!NOTE]
> Each service maintains its own independent in-memory database to ensure service autonomy and strict separation of concerns.

## Setup Instructions

### Prerequisites
- **Java 25**: The project utilizes modern Java features.
- **Maven 3.9+**: For building and dependency management.
- **Docker & Docker Compose**: Recommended for containerized orchestration.

### Installation
Clone the repository and build the entire project from the root directory:

```bash
mvn clean install
```

## Running the Services

### Option 1: Docker Compose (Recommended)
The project includes a `docker-compose.yml` that handles service orchestration and health-based startup ordering.

```bash
docker-compose up --build
```

### Option 2: Manual Start
If running manually, start the Account Service before the Gateway.

**Account Service:**
```bash
cd account-service
mvn spring-boot:run
```

**Event Gateway:**
```bash
cd gateway
mvn spring-boot:run
```

## Running Tests
The test suite covers idempotency, chronological ordering, and resiliency behavior (simulating service failures).

```bash
mvn test
```

## Resiliency Pattern: Timeout + Retry with Backoff

For the critical communication path between the Gateway and the Account Service, we have implemented the **Timeout + Retry with Backoff** pattern using Project Reactor.

- **Why this choice?**: In a financial system, transient network blips shouldn't result in immediate failure for the customer. However, waiting indefinitely can lead to thread pool exhaustion (cascading failure). 
- **Timeout**: Each request is capped at 2 seconds. This ensures the Gateway remains responsive even if the downstream service hangs.
- **Exponential Backoff**: If a transient error occurs, the system retries up to 2 times with increasing delays (starting at 200ms). This gives the Account Service time to recover without being overwhelmed by a "retry storm."
- **Graceful Degradation**: When retries are exhausted, the system catches the error, logs the failure with the associated Trace ID, and returns a `503 Service Unavailable` to the client, allowing them to retry at a later time while maintaining the event in a `PENDING` state locally.

> [!IMPORTANT]
> Distributed tracing is integrated into the logging layer. Check service logs for `traceId=[UUID]` to follow a single request across both the Gateway and Account Service logs.