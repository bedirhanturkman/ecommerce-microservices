package com.example.paymentservice.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOutboxEventRepository
        extends JpaRepository<PaymentOutboxEvent, UUID> {

    /*
     * Kafka'ya gönderilmeye hazır PENDING event'leri seçer.
     */
    @Query(
            value = """
                    SELECT *
                    FROM public.payment_outbox_events
                    WHERE status = 'PENDING'
                      AND next_attempt_at <= CURRENT_TIMESTAMP
                    ORDER BY created_at
                    FOR UPDATE SKIP LOCKED
                    LIMIT :batchSize
                    """,
            nativeQuery = true
    )
    List<PaymentOutboxEvent> findPendingBatchForUpdate(
            @Param("batchSize") int batchSize
    );

    /*
     * Aynı Payment için aynı event türünün ikinci kez
     * Outbox'a yazılmasını engeller.
     */
    @Modifying
    @Query(
            value = """
                    INSERT INTO public.payment_outbox_events (
                        id,
                        aggregate_type,
                        aggregate_id,
                        event_type,
                        topic,
                        message_key,
                        payload,
                        status,
                        retry_count,
                        next_attempt_at,
                        created_at,
                        published_at,
                        last_error,
                        version
                    )
                    VALUES (
                        :id,
                        :aggregateType,
                        :aggregateId,
                        :eventType,
                        :topic,
                        :messageKey,
                        :payload,
                        'PENDING',
                        0,
                        :now,
                        :now,
                        NULL,
                        NULL,
                        0
                    )
                    ON CONFLICT (
                        aggregate_type,
                        aggregate_id,
                        event_type
                    )
                    DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertPendingIfAbsent(
            @Param("id") UUID id,
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") String aggregateId,
            @Param("eventType") String eventType,
            @Param("topic") String topic,
            @Param("messageKey") String messageKey,
            @Param("payload") String payload,
            @Param("now") Instant now
    );

    /*
     * Retention süresinden eski PUBLISHED kayıtları
     * batch halinde siler.
     *
     * PENDING kayıtlar bu sorguya dahil edilmez.
     */
    @Modifying
    @Query(
            value = """
                    WITH events_to_delete AS (
                        SELECT id
                        FROM public.payment_outbox_events
                        WHERE status = 'PUBLISHED'
                          AND published_at IS NOT NULL
                          AND published_at < :cutoff
                        ORDER BY published_at
                        LIMIT :batchSize
                        FOR UPDATE SKIP LOCKED
                    )
                    DELETE FROM public.payment_outbox_events
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

    /*
     * Toplam PENDING event sayısını döndürür.
     */
    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM public.payment_outbox_events
                    WHERE status = 'PENDING'
                    """,
            nativeQuery = true
    )
    long countPendingEvents();

    /*
     * En uzun süredir bekleyen PENDING event'in
     * oluşturulma zamanını döndürür.
     */
    @Query(
            value = """
                    SELECT MIN(created_at)
                    FROM public.payment_outbox_events
                    WHERE status = 'PENDING'
                    """,
            nativeQuery = true
    )
    Optional<Instant> findOldestPendingCreatedAt();

    /*
     * Belirlenen retry sınırına ulaşmış PENDING
     * event sayısını döndürür.
     */
    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM public.payment_outbox_events
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