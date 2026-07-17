package com.example.inventoryservice.service;

import com.example.commonevents.payment.PaymentFailedEvent;
import com.example.commonevents.payment.PaymentSucceededEvent;
import com.example.inventoryservice.exception.InvalidPaymentResultEventException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaymentResultEventValidator {

    public void validate(
            PaymentSucceededEvent event
    ) {
        if (event == null) {
            throw invalid(
                    "PaymentSucceededEvent cannot be null"
            );
        }

        validateCommonFields(
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

        validateCommonFields(
                event.paymentId(),
                event.orderId(),
                event.customerId(),
                event.amount()
        );

        if (event.failureCode() == null) {
            throw invalid(
                    "Failure code cannot be null"
            );
        }

        if (event.failureReason() == null
                || event.failureReason().isBlank()) {

            throw invalid(
                    "Failure reason cannot be blank"
            );
        }

        if (event.failedAt() == null) {
            throw invalid(
                    "Failed time cannot be null"
            );
        }
    }

    private void validateCommonFields(
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

    private InvalidPaymentResultEventException invalid(
            String message
    ) {
        return new InvalidPaymentResultEventException(
                message
        );
    }
}