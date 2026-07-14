package com.example.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(
        name = "processed_events",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_processed_events_order_id",
                        columnNames = "order_id"
                )
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @CreatedDate
    @Column(
            name = "processed_at",
            nullable = false,
            updatable = false
    )
    private Instant processedAt;
}