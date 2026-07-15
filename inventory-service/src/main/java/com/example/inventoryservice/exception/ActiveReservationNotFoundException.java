package com.example.inventoryservice.exception;

public class ActiveReservationNotFoundException
        extends RuntimeException {

    public ActiveReservationNotFoundException(Long orderId) {
        super(
                "No active inventory reservation found for order: "
                        + orderId
        );
    }
}