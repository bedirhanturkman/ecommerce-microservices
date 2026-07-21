package com.example.inventoryservice.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class InventoryOutboxMaintenanceService {

    private final InventoryOutboxEventRepository
            outboxEventRepository;

    private final long retentionDays;
    private final int cleanupBatchSize;

    public InventoryOutboxMaintenanceService(
            InventoryOutboxEventRepository
                    outboxEventRepository,

            @Value(
                    "${inventory.outbox.maintenance.retention-days}"
            )
            long retentionDays,

            @Value(
                    "${inventory.outbox.maintenance.cleanup-batch-size}"
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
                Instant.now().minus(
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
                    "Expired Inventory Outbox events deleted. "
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