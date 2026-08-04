package com.example.orderservice.saga;

import com.example.orderservice.dto.OrderItemResponse;
import com.example.orderservice.dto.OrderSagaResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.security.OrderAccessContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderSagaQueryService {
    private final OrderRepository orderRepository;
    private final OrderSagaProjectionRepository projectionRepository;
    private final OrderSagaReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public OrderSagaResponse findByOrderId(
            Long orderId,
            OrderAccessContext accessContext
    ) {
        Order order = (accessContext.admin()
                ? orderRepository.findById(orderId)
                : orderRepository.findByIdAndCustomerId(
                        orderId, accessContext.customerId()))
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderSagaResponse.SagaOrder orderDto = new OrderSagaResponse.SagaOrder(
                order.getId(),
                order.getCustomerId(),
                order.getStatus().name(),
                order.getItems().stream().map(this::toItem).toList(),
                order.getTotalPrice(),
                order.getCreatedAt(),
                order.getVersion()
        );

        return projectionRepository.findById(orderId)
                .map(projection -> toResponse(orderDto, projection))
                .orElseGet(() -> fallback(orderDto, orderId));
    }

    private OrderSagaResponse toResponse(
            OrderSagaResponse.SagaOrder order,
            OrderSagaProjection projection
    ) {
        List<OrderSagaResponse.SagaReservation> reservations =
                reservationRepository.findAllByOrderIdOrderById(
                                projection.getOrderId())
                        .stream()
                        .map(value -> new OrderSagaResponse.SagaReservation(
                                value.getReservationId(),
                                value.getOrderId(),
                                value.getProductId(),
                                value.getQuantity(),
                                value.getStatus().name(),
                                value.getReservationVersion()
                        ))
                        .toList();

        return new OrderSagaResponse(
                order,
                new OrderSagaResponse.SagaInventory(
                        projection.getInventoryStatus().name(),
                        projection.getInventoryFailureCode(),
                        projection.getInventoryFailureMessage(),
                        reservations),
                new OrderSagaResponse.SagaPayment(
                        projection.isPaymentExists(),
                        projection.getPaymentId(),
                        projection.getOrderId(),
                        projection.getPaymentAmount(),
                        projection.getPaymentStatus().name(),
                        projection.getPaymentFailureCode(),
                        projection.getPaymentFailureReason()),
                projection.getSagaStatus().name(),
                true,
                projection.getLastUpdatedAt()
        );
    }

    private OrderSagaResponse fallback(
            OrderSagaResponse.SagaOrder order,
            Long orderId
    ) {
        return new OrderSagaResponse(
                order,
                new OrderSagaResponse.SagaInventory(
                        InventorySagaStatus.PENDING.name(), null, null, List.of()),
                new OrderSagaResponse.SagaPayment(
                        false, null, orderId, null,
                        PaymentSagaStatus.NOT_CREATED.name(), null, null),
                SagaStatus.PROCESSING.name(),
                false,
                null
        );
    }

    private OrderItemResponse toItem(OrderItem item) {
        return new OrderItemResponse(
                item.getId(), item.getProductId(), item.getProductName(),
                item.getQuantity(), item.getUnitPrice(), item.getTotalPrice());
    }
}
