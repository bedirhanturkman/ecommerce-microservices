package com.example.orderservice.consumer;

import com.example.commonevents.payment.PaymentFailedEvent;
import com.example.orderservice.service.OrderResultEventValidator;
import com.example.orderservice.service.OrderResultProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFailedConsumer {

    private final OrderResultEventValidator validator;
    private final OrderResultProcessingService
            processingService;

    @KafkaListener(
            topics =
                    "${order.kafka.topics.payment-failed}",
            containerFactory =
                    "paymentFailedKafkaListenerContainerFactory"
    )
    public void consume(
            PaymentFailedEvent event
    ) {
        log.info(
                "PaymentFailedEvent received by Order Service. "
                        + "paymentId={}, orderId={}, failureCode={}",
                event.paymentId(),
                event.orderId(),
                event.failureCode()
        );

        validator.validate(event);

        boolean updated =
                processingService
                        .markAsPaymentFailed(event);

        if (!updated) {
            log.info(
                    "Order already marked as PAYMENT_FAILED. "
                            + "orderId={}",
                    event.orderId()
            );

            return;
        }

        log.info(
                "Order marked as PAYMENT_FAILED. orderId={}",
                event.orderId()
        );
    }
}