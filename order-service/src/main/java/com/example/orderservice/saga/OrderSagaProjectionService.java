package com.example.orderservice.saga;

import com.example.commonevents.inventory.*;
import com.example.commonevents.payment.PaymentFailedEvent;
import com.example.commonevents.payment.PaymentSucceededEvent;
import com.example.orderservice.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderSagaProjectionService {
    private final OrderSagaProjectionRepository projectionRepository;
    private final OrderSagaReservationRepository reservationRepository;
    private final ProcessedSagaEventRepository processedEventRepository;

    @Transactional
    public void initialize(Long orderId) {
        if (projectionRepository.existsById(orderId)) {
            return;
        }
        projectionRepository.save(OrderSagaProjection.builder()
                .orderId(orderId)
                .inventoryStatus(InventorySagaStatus.PENDING)
                .paymentExists(false)
                .paymentStatus(PaymentSagaStatus.NOT_CREATED)
                .sagaStatus(SagaStatus.PROCESSING)
                .lastUpdatedAt(Instant.now())
                .build());
    }

    @Transactional
    public boolean applyReserved(UUID eventId, InventoryReservedEvent event) {
        if (!claim(eventId, event.getClass().getName(), event.orderId())) {
            return false;
        }
        OrderSagaProjection projection = getOrInitialize(event.orderId());
        if (projection.getInventoryStatus() == InventorySagaStatus.PENDING) {
            projection.setInventoryStatus(InventorySagaStatus.RESERVED);
        }
        upsertReservations(event.orderId(), event.reservedItems().stream()
                .map(item -> new InventoryReservationItem(
                        item.reservationId(), item.productId(), item.quantity(),
                        item.status(), item.version()))
                .toList());
        touchAndRecalculate(projection, null);
        return true;
    }

    @Transactional
    public boolean applyInventoryFailed(
            UUID eventId,
            InventoryReservationFailedEvent event,
            OrderStatus orderStatus
    ) {
        if (!claim(eventId, event.getClass().getName(), event.orderId())) {
            return false;
        }
        OrderSagaProjection projection = getOrInitialize(event.orderId());
        if (!isInventoryTerminal(projection.getInventoryStatus())) {
            projection.setInventoryStatus(InventorySagaStatus.FAILED);
            projection.setInventoryFailureCode(event.errorCode().name());
            projection.setInventoryFailureMessage(event.message());
        }
        touchAndRecalculate(projection, orderStatus);
        return true;
    }

    @Transactional
    public boolean applyConfirmed(
            UUID eventId,
            InventoryReservationConfirmedEvent event,
            OrderStatus orderStatus
    ) {
        if (!claim(eventId, event.getClass().getName(), event.orderId())) {
            return false;
        }
        OrderSagaProjection projection = getOrInitialize(event.orderId());
        if (projection.getInventoryStatus() != InventorySagaStatus.RELEASED
                && projection.getInventoryStatus() != InventorySagaStatus.FAILED) {
            projection.setInventoryStatus(InventorySagaStatus.CONFIRMED);
            upsertReservations(event.orderId(), event.reservations());
        }
        touchAndRecalculate(projection, orderStatus);
        return true;
    }

    @Transactional
    public boolean applyReleased(
            UUID eventId,
            InventoryReservationReleasedEvent event,
            OrderStatus orderStatus
    ) {
        if (!claim(eventId, event.getClass().getName(), event.orderId())) {
            return false;
        }
        OrderSagaProjection projection = getOrInitialize(event.orderId());
        if (projection.getInventoryStatus() != InventorySagaStatus.CONFIRMED
                && projection.getInventoryStatus() != InventorySagaStatus.FAILED) {
            projection.setInventoryStatus(InventorySagaStatus.RELEASED);
            upsertReservations(event.orderId(), event.reservations());
        }
        touchAndRecalculate(projection, orderStatus);
        return true;
    }

    @Transactional
    public boolean applyPaymentSucceeded(
            UUID eventId,
            PaymentSucceededEvent event,
            OrderStatus orderStatus
    ) {
        if (!claim(eventId, event.getClass().getName(), event.orderId())) {
            return false;
        }
        OrderSagaProjection projection = getOrInitialize(event.orderId());
        if (projection.getPaymentStatus() == PaymentSagaStatus.NOT_CREATED) {
            projection.setPaymentExists(true);
            projection.setPaymentId(event.paymentId());
            projection.setPaymentAmount(event.amount());
            projection.setPaymentStatus(PaymentSagaStatus.SUCCEEDED);
        }
        touchAndRecalculate(projection, orderStatus);
        return true;
    }

    @Transactional
    public boolean applyPaymentFailed(
            UUID eventId,
            PaymentFailedEvent event,
            OrderStatus orderStatus
    ) {
        if (!claim(eventId, event.getClass().getName(), event.orderId())) {
            return false;
        }
        OrderSagaProjection projection = getOrInitialize(event.orderId());
        if (projection.getPaymentStatus() == PaymentSagaStatus.NOT_CREATED) {
            projection.setPaymentExists(true);
            projection.setPaymentId(event.paymentId());
            projection.setPaymentAmount(event.amount());
            projection.setPaymentStatus(PaymentSagaStatus.FAILED);
            projection.setPaymentFailureCode(event.failureCode().name());
            projection.setPaymentFailureReason(event.failureReason());
        }
        touchAndRecalculate(projection, orderStatus);
        return true;
    }

    private boolean claim(UUID eventId, String eventType, Long orderId) {
        if (processedEventRepository.existsById(eventId)) {
            return false;
        }
        processedEventRepository.save(ProcessedSagaEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .orderId(orderId)
                .processedAt(Instant.now())
                .build());
        return true;
    }

    private OrderSagaProjection getOrInitialize(Long orderId) {
        return projectionRepository.findById(orderId).orElseGet(() ->
                projectionRepository.save(OrderSagaProjection.builder()
                        .orderId(orderId)
                        .inventoryStatus(InventorySagaStatus.PENDING)
                        .paymentExists(false)
                        .paymentStatus(PaymentSagaStatus.NOT_CREATED)
                        .sagaStatus(SagaStatus.PROCESSING)
                        .lastUpdatedAt(Instant.now())
                        .build()));
    }

    private void upsertReservations(
            Long orderId,
            List<InventoryReservationItem> items
    ) {
        Instant now = Instant.now();
        for (InventoryReservationItem item : items) {
            OrderSagaReservation reservation = reservationRepository
                    .findByOrderIdAndProductId(orderId, item.productId())
                    .orElseGet(() -> OrderSagaReservation.builder()
                            .orderId(orderId)
                            .productId(item.productId())
                            .createdAt(now)
                            .build());

            ReservationSagaStatus incoming =
                    ReservationSagaStatus.valueOf(item.status());
            if (reservation.getStatus() == null
                    || statusRank(incoming) >= statusRank(reservation.getStatus())) {
                reservation.setReservationId(item.reservationId());
                reservation.setQuantity(item.quantity());
                reservation.setStatus(incoming);
                reservation.setReservationVersion(item.version());
                reservation.setUpdatedAt(now);
                reservationRepository.save(reservation);
            }
        }
    }

    private int statusRank(ReservationSagaStatus status) {
        return switch (status) {
            case RESERVED -> 0;
            case CONFIRMED, RELEASED -> 1;
        };
    }

    private boolean isInventoryTerminal(InventorySagaStatus status) {
        return status == InventorySagaStatus.CONFIRMED
                || status == InventorySagaStatus.RELEASED
                || status == InventorySagaStatus.FAILED;
    }

    private void touchAndRecalculate(
            OrderSagaProjection projection,
            OrderStatus orderStatus
    ) {
        boolean complete = projection.getInventoryStatus() == InventorySagaStatus.FAILED
                && orderStatus == OrderStatus.INVENTORY_FAILED
                || projection.getInventoryStatus() == InventorySagaStatus.CONFIRMED
                && projection.getPaymentStatus() == PaymentSagaStatus.SUCCEEDED
                && orderStatus == OrderStatus.PAID
                || projection.getInventoryStatus() == InventorySagaStatus.RELEASED
                && projection.getPaymentStatus() == PaymentSagaStatus.FAILED
                && orderStatus == OrderStatus.PAYMENT_FAILED;
        projection.setSagaStatus(complete ? SagaStatus.COMPLETED : SagaStatus.PROCESSING);
        projection.setLastUpdatedAt(Instant.now());
    }
}
