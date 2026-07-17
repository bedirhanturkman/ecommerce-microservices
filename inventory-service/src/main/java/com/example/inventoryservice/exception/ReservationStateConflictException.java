package com.example.inventoryservice.exception;

import com.example.inventoryservice.entity.ReservationStatus;

public class ReservationStateConflictException
        extends RuntimeException {

    public ReservationStateConflictException(
            Long orderId,
            ReservationStatus currentStatus,
            ReservationStatus requestedStatus
    ) {
        super(
                "Reservation state conflict. orderId="
                        + orderId
                        + ", currentStatus="
                        + currentStatus
                        + ", requestedStatus="
                        + requestedStatus
        );
    }
}