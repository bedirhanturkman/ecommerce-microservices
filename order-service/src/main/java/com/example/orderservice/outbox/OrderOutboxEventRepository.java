package com.example.orderservice.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
}