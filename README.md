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

🚧 Under development

The project is being built incrementally to understand each architectural component rather than assembling the system through generated code.