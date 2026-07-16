package com.example.paymentservice.gateway;

import com.example.commonevents.payment.PaymentFailureCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SimulatedPaymentGateway
        implements PaymentGateway {

    private final BigDecimal failureThreshold;

    public SimulatedPaymentGateway(
            @Value(
                    "${payment.processing.simulation.failure-threshold}"
            )
            BigDecimal failureThreshold
    ) {
        this.failureThreshold = failureThreshold;
    }

    @Override
    public PaymentGatewayResult charge(
            Long orderId,
            Long customerId,
            BigDecimal amount
    ) {
        if (amount.compareTo(failureThreshold) >= 0) {
            return PaymentGatewayResult.failure(
                    PaymentFailureCode.PAYMENT_DECLINED,
                    "Payment was declined by the simulated payment gateway"
            );
        }

        return PaymentGatewayResult.success();
    }
}