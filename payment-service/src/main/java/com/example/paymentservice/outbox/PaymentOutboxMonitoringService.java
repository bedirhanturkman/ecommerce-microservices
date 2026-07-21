package com.example.paymentservice.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class PaymentOutboxMonitoringService {

    private final PaymentOutboxEventRepository
            outboxEventRepository;

    private final int highRetryThreshold;
    private final long oldestPendingWarningSeconds;

    public PaymentOutboxMonitoringService(
            PaymentOutboxEventRepository
                    outboxEventRepository,

            @Value(
                    "${payment.outbox.monitoring.high-retry-threshold}"
            )
            int highRetryThreshold,

            @Value(
                    "${payment.outbox.monitoring.oldest-pending-warning-seconds}"
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
    public PaymentOutboxHealthSnapshot
    collectSnapshot() {

        long pendingCount =
                outboxEventRepository
                        .countPendingEvents();

        Optional<Instant> oldestPending =
                outboxEventRepository
                        .findOldestPendingCreatedAt();

        long oldestPendingAgeSeconds =
                oldestPending
                        .map(this::calculateAgeSeconds)
                        .orElse(0L);

        long highRetryPendingCount =
                outboxEventRepository
                        .countPendingEventsWithHighRetry(
                                highRetryThreshold
                        );

        return new PaymentOutboxHealthSnapshot(
                pendingCount,
                oldestPending.orElse(null),
                oldestPendingAgeSeconds,
                highRetryPendingCount
        );
    }

    public void logSnapshot(
            PaymentOutboxHealthSnapshot snapshot
    ) {
        if (snapshot.pendingCount() == 0) {
            log.debug(
                    "Payment Outbox is healthy. pendingCount=0"
            );

            return;
        }

        boolean oldPendingExists =
                snapshot.oldestPendingAgeSeconds()
                        >= oldestPendingWarningSeconds;

        boolean highRetryExists =
                snapshot.highRetryPendingCount() > 0;

        if (oldPendingExists || highRetryExists) {
            log.warn(
                    "Payment Outbox requires attention. "
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
                "Payment Outbox contains pending events. "
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