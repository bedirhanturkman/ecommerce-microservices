package com.example.paymentservice.event.listener;

import com.example.commonevents.payment.PaymentFailedEvent;
import com.example.commonevents.payment.PaymentSucceededEvent;
import com.example.paymentservice.event.internal.PaymentFailedInternalEvent;
import com.example.paymentservice.event.internal.PaymentSucceededInternalEvent;
import com.example.paymentservice.producer.PaymentEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentResultEventListener {

    private final PaymentEventProducer paymentEventProducer;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handlePaymentSucceeded(
            PaymentSucceededInternalEvent event
    ) {
        PaymentSucceededEvent kafkaEvent =
                new PaymentSucceededEvent(
                        event.paymentId(),
                        event.orderId(),
                        event.customerId(),
                        event.amount(),
                        event.succeededAt()
                );

        paymentEventProducer.publishPaymentSucceeded(
                kafkaEvent
        );
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handlePaymentFailed(
            PaymentFailedInternalEvent event
    ) {
        PaymentFailedEvent kafkaEvent =
                new PaymentFailedEvent(
                        event.paymentId(),
                        event.orderId(),
                        event.customerId(),
                        event.amount(),
                        event.failureCode(),
                        event.failureReason(),
                        event.failedAt()
                );

        paymentEventProducer.publishPaymentFailed(
                kafkaEvent
        );
    }
}