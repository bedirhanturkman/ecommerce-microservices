package com.example.orderservice.saga;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderSagaProjectionRepository
        extends JpaRepository<OrderSagaProjection, Long> {
}
