# System Architecture

## Overview

The project follows a microservices architecture in which business capabilities are separated into independently deployable services.

## Implementation Status

- **Discovery Server:** Implemented with Netflix Eureka. Runs on port `8761` and is configured for standalone operation.
- **API Gateway:** Implemented with Spring Cloud Gateway WebFlux. Runs on port `8080`, registers with Eureka, and has discovery-based routing enabled.
- **Product Service:** Planned.
- **Order Service:** Planned.
- **Service-to-service communication:** OpenFeign and Kafka are planned; they will be introduced when the relevant services are implemented.
- **Persistence and infrastructure:** Separate PostgreSQL databases and Docker Compose are planned.

## Services

### Discovery Server

Responsible for service registration and discovery using Netflix Eureka.

### API Gateway

Acts as the single entry point for external clients. It registers with Eureka and uses service discovery to create routes for registered backend services. Request routing to a backend will be verified once a backend service is implemented.

### Product Service

Responsible for product-related operations and inventory information.

### Order Service

Responsible for creating and managing orders. It communicates with the Product Service when inventory validation is required.

## Communication

### Synchronous

Spring Cloud OpenFeign will be used when an immediate response from another service is required.

Example:

```text
Order Service
      |
      | HTTP / OpenFeign
      ↓
Product Service
      |
      ↓
Inventory result