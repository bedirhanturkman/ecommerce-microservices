package com.example.inventoryservice.outbox;

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
        prefix = "inventory.outbox.maintenance",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class InventoryOutboxMaintenanceScheduler {

    private final InventoryOutboxMaintenanceService
            maintenanceService;

    @Scheduled(
            fixedDelayString =
                    "${inventory.outbox.maintenance.fixed-delay-ms}",
            initialDelayString =
                    "${inventory.outbox.maintenance.initial-delay-ms}"
    )
    public void cleanExpiredPublishedEvents() {
        try {
            int deletedCount =
                    maintenanceService
                            .deleteExpiredPublishedEvents();

            log.info(
                    "Inventory Outbox maintenance cycle completed. "
                            + "deletedCount={}",
                    deletedCount
            );

        } catch (Exception exception) {
            log.error(
                    "Inventory Outbox maintenance cycle failed",
                    exception
            );
        }
    }
}