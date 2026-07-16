package com.example.paymentservice.exception;

public class PaymentNotFoundException
        extends RuntimeException {

    private PaymentNotFoundException(
            String message
    ) {
        super(message);
    }

    public static PaymentNotFoundException byPaymentId(
            Long paymentId
    ) {
        return new PaymentNotFoundException(
                "Payment not found. paymentId="
                        + paymentId
        );
    }

    public static PaymentNotFoundException byOrderId(
            Long orderId
    ) {
        return new PaymentNotFoundException(
                "Payment not found. orderId="
                        + orderId
        );
    }
}