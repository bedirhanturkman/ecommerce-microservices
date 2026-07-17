package com.example.orderservice.service;

import com.example.commonevents.inventory.InventoryReservationFailedEvent;
import com.example.commonevents.payment.PaymentFailedEvent;
import com.example.commonevents.payment.PaymentSucceededEvent;
import com.example.orderservice.exception.InvalidOrderResultEventException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderResultEventValidator {

    public void validate(
            PaymentSucceededEvent event
    ) {
        if (event == null) {
            throw invalid(
                    "PaymentSucceededEvent cannot be null"
            );
        }

        validatePaymentFields(
                event.paymentId(),
                event.orderId(),
                event.customerId(),
                event.amount()
        );

        if (event.succeededAt() == null) {
            throw invalid(
                    "Succeeded time cannot be null"
            );
        }
    }

    public void validate(
            PaymentFailedEvent event
    ) {
        if (event == null) {
            throw invalid(
                    "PaymentFailedEvent cannot be null"
            );
        }

        validatePaymentFields(
                event.paymentId(),
                event.orderId(),
                event.customerId(),
                event.amount()
        );

        if (event.failureCode() == null) {
            throw invalid(
                    "Payment failure code cannot be null"
            );
        }

        if (event.failureReason() == null
                || event.failureReason().isBlank()) {

            throw invalid(
                    "Payment failure reason cannot be blank"
            );
        }

        if (event.failedAt() == null) {
            throw invalid(
                    "Payment failure time cannot be null"
            );
        }
    }

    public void validate(
            InventoryReservationFailedEvent event
    ) {
        if (event == null) {
            throw invalid(
                    "InventoryReservationFailedEvent cannot be null"
            );
        }

        if (event.orderId() == null) {
            throw invalid(
                    "Order id cannot be null"
            );
        }

        if (event.errorCode() == null) {
            throw invalid(
                    "Inventory reservation error code cannot be null"
            );
        }

        if (event.message() == null
                || event.message().isBlank()) {

            throw invalid(
                    "Inventory reservation failure message cannot be blank"
            );
        }

        if (event.failedAt() == null) {
            throw invalid(
                    "Inventory reservation failure time cannot be null"
            );
        }
    }

    private void validatePaymentFields(
            Long paymentId,
            Long orderId,
            Long customerId,
            BigDecimal amount
    ) {
        if (paymentId == null) {
            throw invalid(
                    "Payment id cannot be null"
            );
        }

        if (orderId == null) {
            throw invalid(
                    "Order id cannot be null"
            );
        }

        if (customerId == null) {
            throw invalid(
                    "Customer id cannot be null"
            );
        }

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw invalid(
                    "Payment amount must be greater than zero"
            );
        }
    }

    private InvalidOrderResultEventException invalid(
            String message
    ) {
        return new InvalidOrderResultEventException(
                message
        );
    }
}