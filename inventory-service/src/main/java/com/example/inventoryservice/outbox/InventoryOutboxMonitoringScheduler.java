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
        prefix = "inventory.outbox.monitoring",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class InventoryOutboxMonitoringScheduler {

    private final InventoryOutboxMonitoringService
            monitoringService;

    @Scheduled(
            fixedDelayString =
                    "${inventory.outbox.monitoring.fixed-delay-ms}",
            initialDelayString =
                    "${inventory.outbox.monitoring.initial-delay-ms}"
    )
    public void monitorOutbox() {
        try {
            InventoryOutboxHealthSnapshot snapshot =
                    monitoringService.collectSnapshot();

            monitoringService.logSnapshot(snapshot);

        } catch (Exception exception) {
            log.error(
                    "Inventory Outbox monitoring cycle failed",
                    exception
            );
        }
    }
}