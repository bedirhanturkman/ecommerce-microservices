package com.example.orderservice.saga;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_saga_projections", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSagaProjection {
    @Id
    @Column(name = "order_id")
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_status", nullable = false)
    private InventorySagaStatus inventoryStatus;

    @Column(name = "inventory_failure_code")
    private String inventoryFailureCode;

    @Column(name = "inventory_failure_message", length = 1000)
    private String inventoryFailureMessage;

    @Column(name = "payment_exists", nullable = false)
    private boolean paymentExists;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "payment_amount", precision = 19, scale = 2)
    private BigDecimal paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentSagaStatus paymentStatus;

    @Column(name = "payment_failure_code")
    private String paymentFailureCode;

    @Column(name = "payment_failure_reason", length = 500)
    private String paymentFailureReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_status", nullable = false)
    private SagaStatus sagaStatus;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
