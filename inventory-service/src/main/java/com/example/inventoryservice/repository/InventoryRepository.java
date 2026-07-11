package com.example.inventoryservice.repository;

import com.example.inventoryservice.document.Inventory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface InventoryRepository
        extends MongoRepository<Inventory, String> {

    Optional<Inventory> findByProductId(String productId);

    boolean existsByProductId(String productId);
}