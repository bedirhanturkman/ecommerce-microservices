# E-Commerce Microservices Backend

Spring Boot microservice mimaride geliştirilen e-ticaret backend projesidir.

## Amaç

Bu proje staj sürecinde, Spring Boot ve Spring Cloud kullanarak gerçek hayata yakın bir microservice backend mimarisi kurmak amacıyla geliştirilmiştir.

## Servisler

- eureka-server
- config-server
- api-gateway
- auth-service
- customer-service
- product-service
- order-service

## Kullanılan Teknolojiler

- Java
- Spring Boot
- Spring Cloud
- Spring Cloud Gateway
- Netflix Eureka
- Spring Cloud Config Server
- Spring Security
- OAuth2 Resource Server
- PostgreSQL
- MongoDB
- Docker
- Maven
- Lombok

## Mimari

Client / Postman
→ API Gateway
→ Microservices
→ Her servisin kendi veritabanı

## Çalıştırma Sırası

1. eureka-server
2. config-server
3. customer-service
4. auth-service
5. product-service
6. order-service
7. api-gateway

## Mevcut Durum

- Customer Service Resource Server yapısına geçirildi.
- Auth Service JWT üretiminden sorumlu hale getirildi.
- Config Server ile merkezi konfigürasyon yönetimi kuruldu.
- Eureka ile servis keşfi sağlandı.
- API Gateway ile merkezi yönlendirme yapıldı.

## Roadmap

- Product Service Resource Server yapısına geçirilecek.
- Order Service Resource Server yapısına geçirilecek.
- SELLER rolü eklenecek.
- Rate Limiting ve Circuit Breaker eklenecek.
