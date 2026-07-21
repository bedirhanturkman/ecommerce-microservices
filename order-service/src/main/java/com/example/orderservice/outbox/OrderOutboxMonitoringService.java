package com.example.orderservice.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class OrderOutboxMonitoringService {

    private final OrderOutboxEventRepository
            outboxEventRepository;

    private final int highRetryThreshold;
    private final long oldestPendingWarningSeconds;

    public OrderOutboxMonitoringService(
            OrderOutboxEventRepository
                    outboxEventRepository,

            @Value(
                    "${order.outbox.monitoring.high-retry-threshold}"
            )
            int highRetryThreshold,

            @Value(
                    "${order.outbox.monitoring.oldest-pending-warning-seconds}"
            )
            long oldestPendingWarningSeconds
    ) {
        this.outboxEventRepository =
                outboxEventRepository;

        this.highRetryThreshold =
                highRetryThreshold;

        this.oldestPendingWarningSeconds =
                oldestPendingWarningSeconds;
    }

    @Transactional(readOnly = true)
    public OrderOutboxHealthSnapshot
    collectSnapshot() {

        long pendingCount =
                outboxEventRepository
                        .countPendingEvents();

        Optional<Instant> oldestPending =
                outboxEventRepository
                        .findOldestPendingCreatedAt();

        long oldestPendingAgeSeconds =
                oldestPending
                        .map(
                                createdAt ->
                                        calculateAgeSeconds(
                                                createdAt
                                        )
                        )
                        .orElse(0L);

        long highRetryPendingCount =
                outboxEventRepository
                        .countPendingEventsWithHighRetry(
                                highRetryThreshold
                        );

        return new OrderOutboxHealthSnapshot(
                pendingCount,
                oldestPending.orElse(null),
                oldestPendingAgeSeconds,
                highRetryPendingCount
        );
    }

    public void logSnapshot(
            OrderOutboxHealthSnapshot snapshot
    ) {
        if (snapshot.pendingCount() == 0) {
            log.debug(
                    "Order Outbox is healthy. pendingCount=0"
            );

            return;
        }

        boolean oldestEventExceededThreshold =
                snapshot.oldestPendingAgeSeconds()
                        >= oldestPendingWarningSeconds;

        boolean hasHighRetryEvents =
                snapshot.highRetryPendingCount() > 0;

        if (oldestEventExceededThreshold
                || hasHighRetryEvents) {

            log.warn(
                    "Order Outbox requires attention. "
                            + "pendingCount={}, "
                            + "oldestPendingCreatedAt={}, "
                            + "oldestPendingAgeSeconds={}, "
                            + "highRetryPendingCount={}, "
                            + "highRetryThreshold={}",
                    snapshot.pendingCount(),
                    snapshot.oldestPendingCreatedAt(),
                    snapshot.oldestPendingAgeSeconds(),
                    snapshot.highRetryPendingCount(),
                    highRetryThreshold
            );

            return;
        }

        log.info(
                "Order Outbox contains pending events. "
                        + "pendingCount={}, "
                        + "oldestPendingAgeSeconds={}, "
                        + "highRetryPendingCount={}",
                snapshot.pendingCount(),
                snapshot.oldestPendingAgeSeconds(),
                snapshot.highRetryPendingCount()
        );
    }

    private long calculateAgeSeconds(
            Instant createdAt
    ) {
        long ageSeconds =
                Duration.between(
                        createdAt,
                        Instant.now()
                ).toSeconds();

        return Math.max(
                ageSeconds,
                0L
        );
    }
}