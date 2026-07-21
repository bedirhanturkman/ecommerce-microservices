package com.example.paymentservice.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentOutboxEventRepository
        extends JpaRepository<PaymentOutboxEvent, UUID> {

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
}