# E-Commerce Microservices Backend

A Spring Boot based e-commerce backend built with a Microservice Architecture.

## Architecture

- API Gateway
- Config Server
- Eureka Server
- Auth Service
- Customer Service (PostgreSQL)
- Order Service (PostgreSQL)
- Product Service (MongoDB)
- Apache Kafka

## Technologies

- Java 25
- Spring Boot 4
- Spring Security
- OAuth2 Resource Server
- JWT Authentication
- Spring Cloud Gateway
- Spring Cloud Config
- Netflix Eureka
- Spring Data JPA
- Spring Data MongoDB
- PostgreSQL
- MongoDB
- Apache Kafka
- Docker
- MapStruct
- Maven

## Current Features

### Authentication

- JWT based authentication
- Login / Register
- Password encryption
- Role based authorization

### Customer Service

- Customer management
- Internal APIs
- Resource Server
- MapStruct mapping
- Permission abstraction

### Product Service

- MongoDB based product management
- Resource Server
- Role based authorization
- MapStruct

### Order Service

- Order creation
- Transaction management
- OrderStatus enum
- ProductClient abstraction
- Resource Server
- MapStruct

### Event Driven

- Kafka Producer
- OrderCreatedEvent publishing
- order-created topic

## Security

Supported Roles

- USER
- SELLER
- ADMIN

Authorization is implemented using Spring Security with JWT.

## Service Communication

- REST (current)
- Kafka Producer (implemented)
- Kafka Consumer (next step)

## Databases

| Service | Database |
|----------|----------|
| Customer Service | PostgreSQL |
| Order Service | PostgreSQL |
| Product Service | MongoDB |

## Infrastructure

- Docker
- Config Server
- Eureka Service Discovery
- API Gateway

## Next Steps

- Product Service Kafka Consumer
- Stock update via Kafka
- Retry / DLQ
- Saga Pattern
- Distributed Tracing
