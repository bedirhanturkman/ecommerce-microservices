package com.example.paymentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "processed_payment_events",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_processed_payment_events_order_id",
                        columnNames = "order_id"
                )
        }
)
public class ProcessedPaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "order_id",
            nullable = false,
            updatable = false
    )
    private Long orderId;

    @Column(
            name = "processed_at",
            nullable = false,
            updatable = false
    )
    private Instant processedAt;
}