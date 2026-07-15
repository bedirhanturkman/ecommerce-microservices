package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.InventoryReservation;
import com.example.inventoryservice.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryReservationRepository
        extends JpaRepository<InventoryReservation, Long> {

    List<InventoryReservation> findAllByOrderId(
            Long orderId
    );

    List<InventoryReservation> findAllByOrderIdAndStatus(
            Long orderId,
            ReservationStatus status
    );
}