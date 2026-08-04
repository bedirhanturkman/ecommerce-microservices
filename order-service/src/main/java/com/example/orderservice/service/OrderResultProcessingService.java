package com.example.orderservice.service;

import com.example.commonevents.inventory.InventoryReservationFailedEvent;
import com.example.commonevents.payment.PaymentFailedEvent;
import com.example.commonevents.payment.PaymentSucceededEvent;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.exception.OrderResultMismatchException;
import com.example.orderservice.exception.OrderStateConflictException;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.saga.OrderSagaProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderResultProcessingService {

    private final OrderRepository orderRepository;
    private final OrderSagaProjectionService sagaProjectionService;

    @Transactional
    public boolean markAsPaid(
            java.util.UUID eventId,
            PaymentSucceededEvent event
    ) {
        Order order =
                findOrderForUpdate(
                        event.orderId()
                );

        validatePaymentMatchesOrder(
                order,
                event.customerId(),
                event.amount()
        );

        boolean updated = transition(
                order,
                OrderStatus.PAID
        );
        sagaProjectionService.applyPaymentSucceeded(
                eventId, event, order.getStatus());
        return updated;
    }

    @Transactional
    public boolean markAsPaymentFailed(
            java.util.UUID eventId,
            PaymentFailedEvent event
    ) {
        Order order =
                findOrderForUpdate(
                        event.orderId()
                );

        validatePaymentMatchesOrder(
                order,
                event.customerId(),
                event.amount()
        );

        boolean updated = transition(
                order,
                OrderStatus.PAYMENT_FAILED
        );
        sagaProjectionService.applyPaymentFailed(
                eventId, event, order.getStatus());
        return updated;
    }

    @Transactional
    public boolean markAsInventoryFailed(
            java.util.UUID eventId,
            InventoryReservationFailedEvent event
    ) {
        Order order =
                findOrderForUpdate(
                        event.orderId()
                );

        boolean updated = transition(
                order,
                OrderStatus.INVENTORY_FAILED
        );
        sagaProjectionService.applyInventoryFailed(
                eventId, event, order.getStatus());
        return updated;
    }

    private Order findOrderForUpdate(
            Long orderId
    ) {
        return orderRepository
                .findByIdForUpdate(orderId)
                .orElseThrow(
                        () -> new OrderNotFoundException(
                                orderId
                        )
                );
    }

    private boolean transition(
            Order order,
            OrderStatus requestedStatus
    ) {
        OrderStatus currentStatus =
                order.getStatus();

        /*
         * Aynı event tekrar geldiyse idempotent no-op.
         */
        if (currentStatus == requestedStatus) {
            return false;
        }

        /*
         * Yalnızca CREATED durumundaki sipariş terminal
         * sonuçlardan birine geçirilebilir.
         */
        if (currentStatus != OrderStatus.CREATED) {
            throw new OrderStateConflictException(
                    order.getId(),
                    currentStatus,
                    requestedStatus
            );
        }

        order.setStatus(requestedStatus);

        /*
         * Order managed entity olduğu için save çağrısı
         * gerekmiyor. Commit sırasında dirty checking
         * ile UPDATE sorgusu çalışır.
         */
        return true;
    }

    private void validatePaymentMatchesOrder(
            Order order,
            Long eventCustomerId,
            java.math.BigDecimal eventAmount
    ) {
        if (!order.getCustomerId()
                .equals(eventCustomerId)) {

            throw new OrderResultMismatchException(
                    order.getId(),
                    "Customer id does not match"
            );
        }

        /*
         * Order totalPrice tipi BigDecimal ise bu kod
         * doğrudan kullanılabilir.
         */
        if (order.getTotalPrice()
                .compareTo(eventAmount) != 0) {

            throw new OrderResultMismatchException(
                    order.getId(),
                    "Payment amount does not match order total"
            );
        }
    }
}
