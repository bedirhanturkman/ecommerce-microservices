package com.example.inventoryservice.mapper;

import com.example.commonevents.inventory.InventoryReservationErrorCode;
import com.example.commonevents.inventory.InventoryReservationFailedEvent;
import com.example.commonevents.order.OrderCreatedEvent;
import com.example.inventoryservice.exception.ActiveReservationNotFoundException;
import com.example.inventoryservice.exception.InsufficientStockException;
import com.example.inventoryservice.exception.InvalidOrderCreatedEventException;
import com.example.inventoryservice.exception.InvalidStockQuantityException;
import com.example.inventoryservice.exception.InventoryNotFoundException;
import com.example.inventoryservice.exception.ReservationAlreadyExistsException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class InventoryReservationFailureEventFactory {

    public InventoryReservationFailedEvent create(
            OrderCreatedEvent event,
            Exception exception
    ) {
        Throwable rootCause =
                NestedExceptionUtils.getMostSpecificCause(
                        exception
                );

        InventoryReservationErrorCode errorCode =
                resolveErrorCode(rootCause);

        return new InventoryReservationFailedEvent(
                event.orderId(),
                errorCode,
                rootCause.getMessage(),
                Instant.now()
        );
    }

    private InventoryReservationErrorCode resolveErrorCode(
            Throwable exception
    ) {
        if (exception instanceof InsufficientStockException) {
            return InventoryReservationErrorCode
                    .INSUFFICIENT_STOCK;
        }

        if (exception instanceof InventoryNotFoundException) {
            return InventoryReservationErrorCode
                    .PRODUCT_NOT_FOUND;
        }

        if (exception instanceof ReservationAlreadyExistsException) {
            return InventoryReservationErrorCode
                    .RESERVATION_ALREADY_EXISTS;
        }

        if (exception instanceof ActiveReservationNotFoundException) {
            return InventoryReservationErrorCode
                    .ACTIVE_RESERVATION_NOT_FOUND;
        }

        if (exception instanceof InvalidOrderCreatedEventException
                || exception instanceof InvalidStockQuantityException
                || exception instanceof IllegalArgumentException) {

            return InventoryReservationErrorCode
                    .INVALID_EVENT;
        }

        return InventoryReservationErrorCode.SYSTEM_ERROR;
    }
}