package com.example.paymentservice.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment_outbox_events",
        schema = "public"
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOutboxEvent {

    @Id
    private UUID id;

    @Column(
            name = "aggregate_type",
            nullable = false,
            length = 100
    )
    private String aggregateType;

    @Column(
            name = "aggregate_id",
            nullable = false,
            length = 100
    )
    private String aggregateId;

    @Column(
            name = "event_type",
            nullable = false,
            length = 150
    )
    private String eventType;

    @Column(
            name = "topic",
            nullable = false,
            length = 150
    )
    private String topic;

    @Column(
            name = "message_key",
            nullable = false,
            length = 150
    )
    private String messageKey;

    @Column(
            name = "payload",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private PaymentOutboxStatus status;

    @Column(
            name = "retry_count",
            nullable = false
    )
    private Integer retryCount;

    @Column(
            name = "next_attempt_at",
            nullable = false
    )
    private Instant nextAttemptAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(
            name = "last_error",
            length = 1000
    )
    private String lastError;

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;
}