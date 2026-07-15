package com.example.inventoryservice.exception;

public class ReservationAlreadyExistsException
        extends RuntimeException {

    public ReservationAlreadyExistsException(Long orderId) {
        super(
                "Inventory reservation already exists for order: "
                        + orderId
        );
    }
}