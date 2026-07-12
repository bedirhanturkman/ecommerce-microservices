package com.example.inventoryservice.validation;

import com.example.commonevents.order.OrderCreatedEvent;
import com.example.commonevents.order.OrderCreatedItemEvent;
import com.example.inventoryservice.exception.InvalidOrderCreatedEventException;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedEventValidator {

    public void validate(OrderCreatedEvent event) {
        if (event == null) {
            throw new InvalidOrderCreatedEventException(
                    "OrderCreatedEvent cannot be null"
            );
        }

        if (event.orderId() == null) {
            throw new InvalidOrderCreatedEventException(
                    "OrderCreatedEvent orderId cannot be null"
            );
        }

        if (event.customerId() == null) {
            throw new InvalidOrderCreatedEventException(
                    "OrderCreatedEvent customerId cannot be null"
            );
        }

        if (event.status() == null || event.status().isBlank()) {
            throw new InvalidOrderCreatedEventException(
                    "OrderCreatedEvent status cannot be blank"
            );
        }

        if (event.createdAt() == null) {
            throw new InvalidOrderCreatedEventException(
                    "OrderCreatedEvent createdAt cannot be null"
            );
        }

        if (event.items() == null || event.items().isEmpty()) {
            throw new InvalidOrderCreatedEventException(
                    "OrderCreatedEvent must contain at least one item"
            );
        }

        event.items().forEach(this::validateItem);
    }

    private void validateItem(OrderCreatedItemEvent item) {
        if (item == null) {
            throw new InvalidOrderCreatedEventException(
                    "OrderCreatedEvent item cannot be null"
            );
        }

        if (item.productId() == null || item.productId().isBlank()) {
            throw new InvalidOrderCreatedEventException(
                    "Order item productId cannot be blank"
            );
        }

        if (item.quantity() == null || item.quantity() <= 0) {
            throw new InvalidOrderCreatedEventException(
                    "Order item quantity must be greater than zero"
            );
        }
    }
}