# E-Commerce Microservices

A learning-focused e-commerce system built with Java, Spring Boot, and a microservices architecture.

## Goal

The goal of this project is to design and implement a clean, distributed e-commerce backend while developing a deep understanding of microservices architecture, service-to-service communication, event-driven systems, containerization, and software engineering practices.

## Architecture

The system will consist of:

- Discovery Server — service registration and discovery using Netflix Eureka
- API Gateway — single entry point for client requests
- Product Service — product and inventory management
- Order Service — order management and inventory validation

### Communication

- OpenFeign — synchronous service-to-service communication
- Apache Kafka — asynchronous event-driven communication

### Infrastructure

- PostgreSQL — isolated database per service
- Docker — containerization
- Docker Compose — local orchestration

## Project Status

Under development. Components are implemented incrementally and verified before integration.

### Implemented Components

- [x] Discovery Server — Netflix Eureka service registry
- [ ] API Gateway
- [ ] Product Service
- [ ] Order Service
- [ ] Database integration
- [ ] Event-driven communication with Kafka
- [ ] Docker Compose orchestration

### Running the Discovery Server

**Prerequisites**
- JDK 21
- Git

Maven is provided through the Maven Wrapper, so a separate Maven installation is not required.

From the repository root, run:

```powershell
cd discovery-server
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

The Eureka dashboard should be available at:

http://localhost:8761

To stop the application, press `Ctrl+C` in the terminal running it.

### Configuration

The Discovery Server uses port `8761`. It is configured not to register itself as a Eureka client or fetch a registry from another server.

### Development Approach

This project is built incrementally to develop a practical understanding of microservices architecture, service discovery, synchronous and asynchronous communication, testing, and deployment.
