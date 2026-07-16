package com.example.paymentservice.gateway;

import com.example.commonevents.payment.PaymentFailureCode;

public record PaymentGatewayResult(
        boolean successful,
        PaymentFailureCode failureCode,
        String failureReason
) {

    public static PaymentGatewayResult success() {
        return new PaymentGatewayResult(
                true,
                null,
                null
        );
    }

    public static PaymentGatewayResult failure(
            PaymentFailureCode failureCode,
            String failureReason
    ) {
        return new PaymentGatewayResult(
                false,
                failureCode,
                failureReason
        );
    }
}