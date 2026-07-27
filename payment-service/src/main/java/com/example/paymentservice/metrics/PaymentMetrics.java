package com.example.paymentservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class PaymentMetrics {

    private final Counter paymentsInitialized;
    private final Counter paymentsAlreadyInitialized;
    private final Counter paymentsSucceeded;
    private final Counter paymentsFailed;
    private final Timer paymentProcessingTimer;

    public PaymentMetrics(
            MeterRegistry meterRegistry
    ) {
        this.paymentsInitialized =
                Counter.builder(
                                "ecommerce.payments.initialized"
                        )
                        .description(
                                "Total number of newly initialized payments"
                        )
                        .register(meterRegistry);

        this.paymentsAlreadyInitialized =
                Counter.builder(
                                "ecommerce.payments.already.initialized"
                        )
                        .description(
                                "Total number of payment events whose payment was already initialized"
                        )
                        .register(meterRegistry);

        this.paymentsSucceeded =
                Counter.builder(
                                "ecommerce.payments.succeeded"
                        )
                        .description(
                                "Total number of successful payment processing results"
                        )
                        .register(meterRegistry);

        this.paymentsFailed =
                Counter.builder(
                                "ecommerce.payments.failed"
                        )
                        .description(
                                "Total number of failed payment processing results"
                        )
                        .register(meterRegistry);

        this.paymentProcessingTimer =
                Timer.builder(
                                "ecommerce.payments.processing"
                        )
                        .description(
                                "Time taken to process a payment"
                        )
                        .register(meterRegistry);
    }

    public <T> T recordPaymentProcessing(
            Supplier<T> operation
    ) {
        return paymentProcessingTimer.record(operation);
    }

    public void incrementPaymentsInitialized() {
        paymentsInitialized.increment();
    }

    public void incrementPaymentsAlreadyInitialized() {
        paymentsAlreadyInitialized.increment();
    }

    public void incrementPaymentsSucceeded() {
        paymentsSucceeded.increment();
    }

    public void incrementPaymentsFailed() {
        paymentsFailed.increment();
    }
}