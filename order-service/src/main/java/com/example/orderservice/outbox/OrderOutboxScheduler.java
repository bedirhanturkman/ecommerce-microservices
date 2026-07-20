package com.example.orderservice.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxScheduler {

    private final OrderOutboxPublishingService
            publishingService;

    @Scheduled(
            fixedDelayString =
                    "${order.outbox.publisher.fixed-delay-ms}",
            initialDelayString =
                    "${order.outbox.publisher.initial-delay-ms}"
    )
    public void publishPendingEvents() {
        try {
            int publishedCount =
                    publishingService
                            .publishPendingBatch();

            if (publishedCount > 0) {
                log.info(
                        "Order outbox publishing cycle completed. "
                                + "publishedCount={}",
                        publishedCount
                );
            }

        } catch (Exception exception) {
            log.error(
                    "Order outbox publishing cycle failed",
                    exception
            );
        }
    }
}