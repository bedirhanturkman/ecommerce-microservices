package com.example.orderservice.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class OrderOutboxMaintenanceService {

    private final OrderOutboxEventRepository
            outboxEventRepository;

    private final long retentionDays;
    private final int cleanupBatchSize;

    public OrderOutboxMaintenanceService(
            OrderOutboxEventRepository
                    outboxEventRepository,

            @Value(
                    "${order.outbox.maintenance.retention-days}"
            )
            long retentionDays,

            @Value(
                    "${order.outbox.maintenance.cleanup-batch-size}"
            )
            int cleanupBatchSize
    ) {
        this.outboxEventRepository =
                outboxEventRepository;

        this.retentionDays =
                retentionDays;

        this.cleanupBatchSize =
                cleanupBatchSize;
    }

    @Transactional
    public int deleteExpiredPublishedEvents() {

        Instant cutoff =
                Instant.now()
                        .minus(
                                retentionDays,
                                ChronoUnit.DAYS
                        );

        int deletedCount =
                outboxEventRepository
                        .deletePublishedBatch(
                                cutoff,
                                cleanupBatchSize
                        );

        if (deletedCount > 0) {
            log.info(
                    "Expired Order Outbox events deleted. "
                            + "deletedCount={}, cutoff={}, "
                            + "retentionDays={}",
                    deletedCount,
                    cutoff,
                    retentionDays
            );
        }

        return deletedCount;
    }
}