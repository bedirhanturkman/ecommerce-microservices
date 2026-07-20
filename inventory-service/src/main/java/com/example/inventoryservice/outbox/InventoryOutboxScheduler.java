package com.example.inventoryservice.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryOutboxScheduler {

    private final InventoryOutboxPublishingService
            publishingService;

    @Scheduled(
            fixedDelayString =
                    "${inventory.outbox.publisher.fixed-delay-ms}",
            initialDelayString =
                    "${inventory.outbox.publisher.initial-delay-ms}"
    )
    public void publishPendingEvents() {
        try {
            int publishedCount =
                    publishingService
                            .publishPendingBatch();

            if (publishedCount > 0) {
                log.info(
                        "Inventory outbox publishing cycle completed. "
                                + "publishedCount={}",
                        publishedCount
                );
            }

        } catch (Exception exception) {
            log.error(
                    "Inventory outbox publishing cycle failed",
                    exception
            );
        }
    }
}