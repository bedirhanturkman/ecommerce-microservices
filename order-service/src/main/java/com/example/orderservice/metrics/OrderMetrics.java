package com.example.orderservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Supplier;

@Component
public class OrderMetrics {

    private final Counter createdOrders;
    private final Counter paidOrders;
    private final Counter inventoryFailedOrders;
    private final Counter paymentFailedOrders;
    private final Timer orderCreationTimer;

    public OrderMetrics(MeterRegistry meterRegistry) {
        this.createdOrders = Counter.builder(
                        "ecommerce.orders.created"
                )
                .description(
                        "Total number of successfully created orders"
                )
                .register(meterRegistry);

        this.paidOrders = Counter.builder(
                        "ecommerce.orders.paid"
                )
                .description(
                        "Total number of orders marked as paid"
                )
                .register(meterRegistry);

        this.inventoryFailedOrders = Counter.builder(
                        "ecommerce.orders.inventory.failed"
                )
                .description(
                        "Total number of orders failed due to inventory reservation"
                )
                .register(meterRegistry);

        this.paymentFailedOrders = Counter.builder(
                        "ecommerce.orders.payment.failed"
                )
                .description(
                        "Total number of orders failed due to payment"
                )
                .register(meterRegistry);

        this.orderCreationTimer = Timer.builder(
                        "ecommerce.orders.creation"
                )
                .description(
                        "Time taken to create an order and its outbox event"
                )
                .register(meterRegistry);
    }

    public <T> T recordOrderCreation(
            Supplier<T> operation
    ) {
        return orderCreationTimer.record(operation);
    }

    /*
     * createOrder metodu @Transactional olduğu için sayaç,
     * transaction başarıyla commit edildikten sonra artırılır.
     *
     * Böylece save veya commit başarısız olursa oluşturulan
     * sipariş sayısı yanlışlıkla artmaz.
     */
    public void incrementCreatedOrdersAfterCommit() {
        incrementAfterCommit(createdOrders);
    }

    public void incrementPaidOrders() {
        paidOrders.increment();
    }

    public void incrementInventoryFailedOrders() {
        inventoryFailedOrders.increment();
    }

    public void incrementPaymentFailedOrders() {
        paymentFailedOrders.increment();
    }

    private void incrementAfterCommit(
            Counter counter
    ) {
        boolean transactionActive =
                TransactionSynchronizationManager
                        .isActualTransactionActive();

        boolean synchronizationActive =
                TransactionSynchronizationManager
                        .isSynchronizationActive();

        if (transactionActive && synchronizationActive) {
            TransactionSynchronizationManager
                    .registerSynchronization(
                            new TransactionSynchronization() {

                                @Override
                                public void afterCommit() {
                                    counter.increment();
                                }
                            }
                    );

            return;
        }

        /*
         * Metot transaction dışında çağrılırsa sayaç
         * doğrudan artırılır.
         */
        counter.increment();
    }
}