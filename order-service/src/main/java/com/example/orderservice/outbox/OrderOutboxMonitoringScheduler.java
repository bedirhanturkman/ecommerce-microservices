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
        prefix = "order.outbox.monitoring",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OrderOutboxMonitoringScheduler {

    private final OrderOutboxMonitoringService
            monitoringService;

    @Scheduled(
            fixedDelayString =
                    "${order.outbox.monitoring.fixed-delay-ms}",
            initialDelayString =
                    "${order.outbox.monitoring.initial-delay-ms}"
    )
    public void monitorOutbox() {
        try {
            OrderOutboxHealthSnapshot snapshot =
                    monitoringService
                            .collectSnapshot();

            monitoringService
                    .logSnapshot(snapshot);

        } catch (Exception exception) {
            log.error(
                    "Order Outbox monitoring cycle failed",
                    exception
            );
        }
    }
}