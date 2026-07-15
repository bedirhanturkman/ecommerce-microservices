package com.example.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(
        name = "inventory_reservations",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_reservation_order_product",
                        columnNames = {"order_id", "product_id"}
                )
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(
            name = "product_id",
            nullable = false,
            length = 255
    )
    private String productId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @CreatedDate
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}