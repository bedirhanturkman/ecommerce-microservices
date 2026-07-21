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
        prefix = "payment.outbox.monitoring",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PaymentOutboxMonitoringScheduler {

    private final PaymentOutboxMonitoringService
            monitoringService;

    @Scheduled(
            fixedDelayString =
                    "${payment.outbox.monitoring.fixed-delay-ms}",
            initialDelayString =
                    "${payment.outbox.monitoring.initial-delay-ms}"
    )
    public void monitorOutbox() {
        try {
            PaymentOutboxHealthSnapshot snapshot =
                    monitoringService.collectSnapshot();

            monitoringService.logSnapshot(snapshot);

        } catch (Exception exception) {
            log.error(
                    "Payment Outbox monitoring cycle failed",
                    exception
            );
        }
    }
}