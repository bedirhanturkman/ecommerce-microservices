package com.example.paymentservice.outbox;

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
        prefix = "payment.outbox.maintenance",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PaymentOutboxMaintenanceScheduler {

    private final PaymentOutboxMaintenanceService
            maintenanceService;

    @Scheduled(
            fixedDelayString =
                    "${payment.outbox.maintenance.fixed-delay-ms}",
            initialDelayString =
                    "${payment.outbox.maintenance.initial-delay-ms}"
    )
    public void cleanExpiredPublishedEvents() {
        try {
            int deletedCount =
                    maintenanceService
                            .deleteExpiredPublishedEvents();

            log.info(
                    "Payment Outbox maintenance cycle completed. "
                            + "deletedCount={}",
                    deletedCount
            );

        } catch (Exception exception) {
            log.error(
                    "Payment Outbox maintenance cycle failed",
                    exception
            );
        }
    }
}