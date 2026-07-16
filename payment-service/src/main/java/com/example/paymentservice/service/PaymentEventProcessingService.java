package com.example.paymentservice.service;

import com.example.commonevents.inventory.InventoryReservedEvent;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.entity.ProcessedPaymentEvent;
import com.example.paymentservice.exception.PaymentAlreadyExistsException;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.repository.ProcessedPaymentEventRepository;
import com.example.paymentservice.service.model.PaymentInitializationResult;
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
    public PaymentInitializationResult initializePayment(
            InventoryReservedEvent event
    ) {
        Long orderId = event.orderId();

        /*
         * Event daha önce işlendi ise mevcut payment kaydı
         * bulunur. Bu kayıt PENDING kaldıysa sonraki aşamada
         * yeniden işlenebilir.
         */
        if (processedPaymentEventRepository
                .existsByOrderId(orderId)) {

            Payment existingPayment =
                    paymentRepository
                            .findByOrderId(orderId)
                            .orElseThrow(
                                    () -> PaymentNotFoundException
                                            .byOrderId(orderId)
                            );

            return new PaymentInitializationResult(
                    existingPayment.getId(),
                    orderId,
                    false
            );
        }

        /*
         * Payment mevcut fakat processed-event yoksa
         * tutarsız bir durum vardır.
         */
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
                        .failureCode(null)
                        .failureReason(null)
                        .completedAt(null)
                        .build();

        ProcessedPaymentEvent processedEvent =
                ProcessedPaymentEvent.builder()
                        .orderId(orderId)
                        .processedAt(Instant.now())
                        .build();

        /*
         * saveAndFlush kullanılmıyor.
         * Transaction tamamlandığında JPA gerekli
         * değişiklikleri veritabanına flush eder.
         */
        Payment savedPayment =
                paymentRepository.save(payment);

        processedPaymentEventRepository.save(
                processedEvent
        );

        return new PaymentInitializationResult(
                savedPayment.getId(),
                orderId,
                true
        );
    }
}