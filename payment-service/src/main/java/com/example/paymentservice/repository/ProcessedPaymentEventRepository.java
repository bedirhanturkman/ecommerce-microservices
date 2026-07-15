package com.example.paymentservice.repository;

import com.example.paymentservice.entity.ProcessedPaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedPaymentEventRepository
        extends JpaRepository<ProcessedPaymentEvent, Long> {

    boolean existsByOrderId(Long orderId);
}