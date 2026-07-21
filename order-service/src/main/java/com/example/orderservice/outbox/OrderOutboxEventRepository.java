package com.example.orderservice.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderOutboxEventRepository
        extends JpaRepository<OrderOutboxEvent, UUID> {

    @Query(
            value = """
                    SELECT *
                    FROM public.order_outbox_events
                    WHERE status = 'PENDING'
                      AND next_attempt_at <= CURRENT_TIMESTAMP
                    ORDER BY created_at
                    FOR UPDATE SKIP LOCKED
                    LIMIT :batchSize
                    """,
            nativeQuery = true
    )
    List<OrderOutboxEvent> findPendingBatchForUpdate(
            @Param("batchSize") int batchSize
    );

    /*
     * Belirlenen tarihten eski PUBLISHED kayıtları batch
     * halinde siler.
     *
     * PENDING kayıtlar sorguya hiçbir şekilde dahil edilmez.
     */
    @Modifying
    @Query(
            value = """
                    WITH events_to_delete AS (
                        SELECT id
                        FROM public.order_outbox_events
                        WHERE status = 'PUBLISHED'
                          AND published_at IS NOT NULL
                          AND published_at < :cutoff
                        ORDER BY published_at
                        FOR UPDATE SKIP LOCKED
                        LIMIT :batchSize
                    )
                    DELETE FROM public.order_outbox_events
                    WHERE id IN (
                        SELECT id
                        FROM events_to_delete
                    )
                    """,
            nativeQuery = true
    )
    int deletePublishedBatch(
            @Param("cutoff") Instant cutoff,
            @Param("batchSize") int batchSize
    );

    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM public.order_outbox_events
                    WHERE status = 'PENDING'
                    """,
            nativeQuery = true
    )
    long countPendingEvents();

    @Query(
            value = """
                    SELECT MIN(created_at)
                    FROM public.order_outbox_events
                    WHERE status = 'PENDING'
                    """,
            nativeQuery = true
    )
    Optional<Instant> findOldestPendingCreatedAt();

    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM public.order_outbox_events
                    WHERE status = 'PENDING'
                      AND retry_count >= :retryThreshold
                    """,
            nativeQuery = true
    )
    long countPendingEventsWithHighRetry(
            @Param("retryThreshold")
            int retryThreshold
    );
}