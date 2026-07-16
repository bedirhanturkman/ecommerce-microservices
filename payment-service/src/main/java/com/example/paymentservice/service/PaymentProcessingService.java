package com.example.paymentservice.service;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.event.internal.PaymentFailedInternalEvent;
import com.example.paymentservice.event.internal.PaymentSucceededInternalEvent;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.gateway.PaymentGateway;
import com.example.paymentservice.gateway.PaymentGatewayResult;
import com.example.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessingService {

    private static final int MAX_FAILURE_REASON_LENGTH =
            500;

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final ApplicationEventPublisher
            applicationEventPublisher;

    @Transactional
    public PaymentStatus processPendingPayment(
            Long paymentId
    ) {
        Payment payment =
                paymentRepository
                        .findByIdForUpdate(paymentId)
                        .orElseThrow(
                                () -> PaymentNotFoundException
                                        .byPaymentId(paymentId)
                        );

        if (payment.getStatus()
                != PaymentStatus.PENDING) {

            log.info(
                    "Payment already completed. " +
                            "paymentId={}, orderId={}, status={}",
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getStatus()
            );

            return payment.getStatus();
        }

        PaymentGatewayResult result =
                paymentGateway.charge(
                        payment.getOrderId(),
                        payment.getCustomerId(),
                        payment.getAmount()
                );

        Instant completedAt = Instant.now();

        if (result.successful()) {
            completeSuccessfully(
                    payment,
                    completedAt
            );

            /*
             * payment managed entity olduğu için ayrıca
             * save veya saveAndFlush çağrısı gerekmiyor.
             */
            applicationEventPublisher.publishEvent(
                    new PaymentSucceededInternalEvent(
                            payment.getId(),
                            payment.getOrderId(),
                            payment.getCustomerId(),
                            payment.getAmount(),
                            completedAt
                    )
            );

            return PaymentStatus.SUCCEEDED;
        }

        completeAsFailed(
                payment,
                result,
                completedAt
        );

        applicationEventPublisher.publishEvent(
                new PaymentFailedInternalEvent(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getCustomerId(),
                        payment.getAmount(),
                        payment.getFailureCode(),
                        payment.getFailureReason(),
                        completedAt
                )
        );

        return PaymentStatus.FAILED;
    }

    private void completeSuccessfully(
            Payment payment,
            Instant completedAt
    ) {
        payment.setStatus(
                PaymentStatus.SUCCEEDED
        );

        payment.setFailureCode(null);
        payment.setFailureReason(null);
        payment.setCompletedAt(completedAt);
    }

    private void completeAsFailed(
            Payment payment,
            PaymentGatewayResult result,
            Instant completedAt
    ) {
        payment.setStatus(
                PaymentStatus.FAILED
        );

        payment.setFailureCode(
                result.failureCode()
        );

        payment.setFailureReason(
                limitFailureReason(
                        result.failureReason()
                )
        );

        payment.setCompletedAt(completedAt);
    }

    private String limitFailureReason(
            String failureReason
    ) {
        if (failureReason == null
                || failureReason.isBlank()) {

            return "Payment failed";
        }

        if (failureReason.length()
                <= MAX_FAILURE_REASON_LENGTH) {

            return failureReason;
        }

        return failureReason.substring(
                0,
                MAX_FAILURE_REASON_LENGTH
        );
    }
}