package com.example.orderservice.saga;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedSagaEventRepository
        extends JpaRepository<ProcessedSagaEvent, UUID> {
}
