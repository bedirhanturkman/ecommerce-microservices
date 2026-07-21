package com.example.paymentservice.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxScheduler {

    private final PaymentOutboxPublishingService
            publishingService;

    @Scheduled(
            fixedDelayString =
                    "${payment.outbox.publisher.fixed-delay-ms}",
            initialDelayString =
                    "${payment.outbox.publisher.initial-delay-ms}"
    )
    public void publishPendingEvents() {
        try {
            int publishedCount =
                    publishingService
                            .publishPendingBatch();

            if (publishedCount > 0) {
                log.info(
                        "Payment outbox publishing cycle completed. "
                                + "publishedCount={}",
                        publishedCount
                );
            }

        } catch (Exception exception) {
            log.error(
                    "Payment outbox publishing cycle failed",
                    exception
            );
        }
    }
}