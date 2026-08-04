package com.example.orderservice.saga;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderSagaReservationRepository
        extends JpaRepository<OrderSagaReservation, Long> {
    List<OrderSagaReservation> findAllByOrderIdOrderById(Long orderId);
    Optional<OrderSagaReservation> findByOrderIdAndProductId(
            Long orderId, String productId);
}
