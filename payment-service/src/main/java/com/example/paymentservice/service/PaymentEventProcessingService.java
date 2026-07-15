package com.example.paymentservice.service;

import com.example.commonevents.inventory.InventoryReservedEvent;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.entity.ProcessedPaymentEvent;
import com.example.paymentservice.exception.PaymentAlreadyExistsException;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.repository.ProcessedPaymentEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentEventProcessingService {

    private final PaymentRepository paymentRepository;

    private final ProcessedPaymentEventRepository
            processedPaymentEventRepository;

    @Transactional
    public boolean process(
            InventoryReservedEvent event
    ) {
        Long orderId = event.orderId();

        if (processedPaymentEventRepository
                .existsByOrderId(orderId)) {

            return false;
        }

        if (paymentRepository.existsByOrderId(orderId)) {
            throw new PaymentAlreadyExistsException(
                    orderId
            );
        }

        Payment payment =
                Payment.builder()
                        .orderId(orderId)
                        .customerId(event.customerId())
                        .amount(event.totalAmount())
                        .status(PaymentStatus.PENDING)
                        .build();

        ProcessedPaymentEvent processedEvent =
                ProcessedPaymentEvent.builder()
                        .orderId(orderId)
                        .processedAt(Instant.now())
                        .build();

        /*
         * İki işlem aynı PostgreSQL transaction içerisindedir.
         * İkinci kayıt başarısız olursa payment kaydı da
         * rollback olur.
         */
        paymentRepository.saveAndFlush(payment);

        processedPaymentEventRepository.saveAndFlush(
                processedEvent
        );

        return true;
    }
}