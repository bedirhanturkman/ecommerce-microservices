package com.example.orderservice.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "order.outbox.maintenance",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OrderOutboxMaintenanceScheduler {

    private final OrderOutboxMaintenanceService
            maintenanceService;

    @Scheduled(
            fixedDelayString =
                    "${order.outbox.maintenance.fixed-delay-ms}",
            initialDelayString =
                    "${order.outbox.maintenance.initial-delay-ms}"
    )
    public void cleanExpiredPublishedEvents() {
        try {
            maintenanceService
                    .deleteExpiredPublishedEvents();

        } catch (Exception exception) {
            log.error(
                    "Order Outbox maintenance cycle failed",
                    exception
            );
        }
    }
}