package com.example.paymentservice.service;

import com.example.commonevents.inventory.InventoryReservedEvent;
import com.example.paymentservice.exception.InvalidInventoryReservedEventException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class InventoryReservedEventValidator {

    public void validate(
            InventoryReservedEvent event
    ) {
        if (event == null) {
            throw new InvalidInventoryReservedEventException(
                    "InventoryReservedEvent cannot be null"
            );
        }

        if (event.orderId() == null) {
            throw new InvalidInventoryReservedEventException(
                    "Order id cannot be null"
            );
        }

        if (event.customerId() == null) {
            throw new InvalidInventoryReservedEventException(
                    "Customer id cannot be null"
            );
        }

        if (event.totalAmount() == null) {
            throw new InvalidInventoryReservedEventException(
                    "Total amount cannot be null"
            );
        }

        if (event.totalAmount()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidInventoryReservedEventException(
                    "Total amount must be greater than zero"
            );
        }

        if (event.reservedItems() == null
                || event.reservedItems().isEmpty()) {

            throw new InvalidInventoryReservedEventException(
                    "Reserved items cannot be empty"
            );
        }
    }
}