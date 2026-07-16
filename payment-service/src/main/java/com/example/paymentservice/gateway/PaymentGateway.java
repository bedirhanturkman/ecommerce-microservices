package com.example.paymentservice.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentGatewayResult charge(
            Long orderId,
            Long customerId,
            BigDecimal amount
    );
}