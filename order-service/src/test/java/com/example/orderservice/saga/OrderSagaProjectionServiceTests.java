package com.example.orderservice.saga;

import com.example.commonevents.inventory.*;
import com.example.commonevents.payment.PaymentFailedEvent;
import com.example.commonevents.payment.PaymentFailureCode;
import com.example.orderservice.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderSagaProjectionServiceTests {
    private final Map<Long, OrderSagaProjection> projections = new HashMap<>();
    private final Set<UUID> processed = new HashSet<>();
    private OrderSagaProjectionRepository projectionRepository;
    private OrderSagaReservationRepository reservationRepository;
    private ProcessedSagaEventRepository processedRepository;
    private OrderSagaProjectionService service;

    @BeforeEach
    void setUp() {
        projectionRepository = mock(OrderSagaProjectionRepository.class);
        reservationRepository = mock(OrderSagaReservationRepository.class);
        processedRepository = mock(ProcessedSagaEventRepository.class);
        when(projectionRepository.findById(anyLong()))
                .thenAnswer(inv -> Optional.ofNullable(projections.get(inv.getArgument(0))));
        when(projectionRepository.existsById(anyLong()))
                .thenAnswer(inv -> projections.containsKey(inv.getArgument(0)));
        when(projectionRepository.save(any())).thenAnswer(inv -> {
            OrderSagaProjection value = inv.getArgument(0);
            projections.put(value.getOrderId(), value);
            return value;
        });
        when(processedRepository.existsById(any()))
                .thenAnswer(inv -> processed.contains(inv.getArgument(0)));
        when(processedRepository.save(any())).thenAnswer(inv -> {
            ProcessedSagaEvent value = inv.getArgument(0);
            processed.add(value.getEventId());
            return value;
        });
        when(reservationRepository.findByOrderIdAndProductId(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new OrderSagaProjectionService(
                projectionRepository, reservationRepository, processedRepository);
    }

    @Test
    void newOrderStartsProcessing() {
        service.initialize(1L);
        OrderSagaProjection projection = projections.get(1L);
        assertThat(projection.getInventoryStatus()).isEqualTo(InventorySagaStatus.PENDING);
        assertThat(projection.getPaymentStatus()).isEqualTo(PaymentSagaStatus.NOT_CREATED);
        assertThat(projection.getSagaStatus()).isEqualTo(SagaStatus.PROCESSING);
    }

    @Test
    void inventoryFailureCompletesWithoutPayment() {
        service.initialize(1L);
        service.applyInventoryFailed(UUID.randomUUID(),
                new InventoryReservationFailedEvent(
                        1L, InventoryReservationErrorCode.INSUFFICIENT_STOCK,
                        "Insufficient stock", Instant.now()),
                OrderStatus.INVENTORY_FAILED);
        OrderSagaProjection projection = projections.get(1L);
        assertThat(projection.getInventoryStatus()).isEqualTo(InventorySagaStatus.FAILED);
        assertThat(projection.getInventoryFailureCode()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(projection.isPaymentExists()).isFalse();
        assertThat(projection.getSagaStatus()).isEqualTo(SagaStatus.COMPLETED);
    }

    @Test
    void paymentFailureNeedsReleaseBeforeCompletion() {
        service.initialize(1L);
        service.applyPaymentFailed(UUID.randomUUID(),
                new PaymentFailedEvent(9L, 1L, 2L, BigDecimal.TEN,
                        PaymentFailureCode.PAYMENT_DECLINED, "declined", Instant.now()),
                OrderStatus.PAYMENT_FAILED);
        assertThat(projections.get(1L).getSagaStatus()).isEqualTo(SagaStatus.PROCESSING);

        service.applyReleased(UUID.randomUUID(),
                new InventoryReservationReleasedEvent(1L, List.of(
                        new InventoryReservationItem(
                                4L, "p1", 2, "RELEASED", 2L)), Instant.now()),
                OrderStatus.PAYMENT_FAILED);
        assertThat(projections.get(1L).getInventoryStatus())
                .isEqualTo(InventorySagaStatus.RELEASED);
        assertThat(projections.get(1L).getSagaStatus()).isEqualTo(SagaStatus.COMPLETED);
    }

    @Test
    void duplicateEventIsNoOp() {
        service.initialize(1L);
        UUID eventId = UUID.randomUUID();
        InventoryReservedEvent event = new InventoryReservedEvent(
                1L, 2L, BigDecimal.TEN,
                List.of(new ReservedInventoryItem(
                        3L, "p1", 1, "RESERVED", 0L)), Instant.now());
        assertThat(service.applyReserved(eventId, event)).isTrue();
        assertThat(service.applyReserved(eventId, event)).isFalse();
        verify(reservationRepository, times(1)).save(any());
    }

    @Test
    void terminalReleaseIsNotRegressedByLateReservedEvent() {
        service.initialize(1L);
        service.applyPaymentFailed(UUID.randomUUID(),
                new PaymentFailedEvent(9L, 1L, 2L, BigDecimal.TEN,
                        PaymentFailureCode.PAYMENT_DECLINED, "declined", Instant.now()),
                OrderStatus.PAYMENT_FAILED);
        service.applyReleased(UUID.randomUUID(),
                new InventoryReservationReleasedEvent(1L, List.of(
                        new InventoryReservationItem(
                                4L, "p1", 1, "RELEASED", 2L)), Instant.now()),
                OrderStatus.PAYMENT_FAILED);
        service.applyReserved(UUID.randomUUID(),
                new InventoryReservedEvent(1L, 2L, BigDecimal.TEN,
                        List.of(new ReservedInventoryItem(
                                4L, "p1", 1, "RESERVED", 0L)), Instant.now()));
        assertThat(projections.get(1L).getInventoryStatus())
                .isEqualTo(InventorySagaStatus.RELEASED);
    }

    @Test
    void multiItemReservationCreatesOneProjectionPerProduct() {
        service.initialize(1L);
        service.applyReserved(UUID.randomUUID(),
                new InventoryReservedEvent(1L, 2L, BigDecimal.TEN, List.of(
                        new ReservedInventoryItem(3L, "p1", 1, "RESERVED", 0L),
                        new ReservedInventoryItem(4L, "p2", 2, "RESERVED", 0L)),
                        Instant.now()));
        ArgumentCaptor<OrderSagaReservation> captor =
                ArgumentCaptor.forClass(OrderSagaReservation.class);
        verify(reservationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(OrderSagaReservation::getProductId)
                .containsExactly("p1", "p2");
    }
}
