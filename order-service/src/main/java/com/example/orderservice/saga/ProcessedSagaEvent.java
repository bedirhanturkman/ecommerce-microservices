package com.example.orderservice.saga;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_saga_processed_events", schema = "public")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedSagaEvent {
    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
