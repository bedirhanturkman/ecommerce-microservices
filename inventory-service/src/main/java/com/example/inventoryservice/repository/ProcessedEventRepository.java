package com.example.inventoryservice.repository;

import com.example.inventoryservice.document.ProcessedEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProcessedEventRepository
        extends MongoRepository<ProcessedEvent, String> {

    boolean existsByOrderId(Long orderId);
}