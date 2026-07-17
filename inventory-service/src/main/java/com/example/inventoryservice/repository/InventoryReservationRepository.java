package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.InventoryReservation;
import com.example.inventoryservice.entity.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reservation
            from InventoryReservation reservation
            where reservation.orderId = :orderId
            order by reservation.id
            """)
    List<InventoryReservation> findAllByOrderIdForUpdate(
            @Param("orderId") Long orderId
    );
}