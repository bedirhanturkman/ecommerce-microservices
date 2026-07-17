package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository
        extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(
            String productId
    );

    boolean existsByProductId(
            String productId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select inventory
            from Inventory inventory
            where inventory.productId = :productId
            """)
    Optional<Inventory> findByProductIdForUpdate(
            @Param("productId") String productId
    );
}