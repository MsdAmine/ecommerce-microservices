# System Architecture

## Overview

The project follows a microservices architecture in which business capabilities are separated into independently deployable services.

## Services

### Discovery Server

Responsible for service registration and discovery using Netflix Eureka.

### API Gateway

Acts as the single entry point for external clients and routes requests to the appropriate backend service.

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