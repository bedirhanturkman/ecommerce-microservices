package com.example.inventoryservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class InventoryMetrics {

    private final Counter reservationsCreated;
    private final Counter reservationsFailed;
    private final Counter reservationsConfirmed;
    private final Counter reservationsReleased;
    private final Timer reservationProcessingTimer;

    public InventoryMetrics(
            MeterRegistry meterRegistry
    ) {
        this.reservationsCreated =
                Counter.builder(
                                "ecommerce.inventory.reservations.created"
                        )
                        .description(
                                "Total number of successfully created inventory reservations"
                        )
                        .register(meterRegistry);

        this.reservationsFailed =
                Counter.builder(
                                "ecommerce.inventory.reservations.failed"
                        )
                        .description(
                                "Total number of failed inventory reservation attempts"
                        )
                        .register(meterRegistry);

        this.reservationsConfirmed =
                Counter.builder(
                                "ecommerce.inventory.reservations.confirmed"
                        )
                        .description(
                                "Total number of confirmed inventory reservations"
                        )
                        .register(meterRegistry);

        this.reservationsReleased =
                Counter.builder(
                                "ecommerce.inventory.reservations.released"
                        )
                        .description(
                                "Total number of released inventory reservations"
                        )
                        .register(meterRegistry);

        this.reservationProcessingTimer =
                Timer.builder(
                                "ecommerce.inventory.reservation.processing"
                        )
                        .description(
                                "Time taken to process an OrderCreated event"
                        )
                        .register(meterRegistry);
    }

    public boolean recordReservationProcessing(
            Supplier<Boolean> operation
    ) {
        Boolean result =
                reservationProcessingTimer.record(operation);

        return Boolean.TRUE.equals(result);
    }

    public void incrementReservationsCreated() {
        reservationsCreated.increment();
    }

    public void incrementReservationsFailed() {
        reservationsFailed.increment();
    }

    public void incrementReservationsConfirmed() {
        reservationsConfirmed.increment();
    }

    public void incrementReservationsReleased() {
        reservationsReleased.increment();
    }
}