package com.example.orderservice.consumer;

import com.example.commonevents.payment.PaymentSucceededEvent;
import com.example.orderservice.service.OrderResultEventValidator;
import com.example.orderservice.service.OrderResultProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSucceededConsumer {

    private final OrderResultEventValidator validator;
    private final OrderResultProcessingService
            processingService;

    @KafkaListener(
            topics =
                    "${order.kafka.topics.payment-succeeded}",
            containerFactory =
                    "paymentSucceededKafkaListenerContainerFactory"
    )
    public void consume(
            PaymentSucceededEvent event
    ) {
        log.info(
                "PaymentSucceededEvent received by Order Service. "
                        + "paymentId={}, orderId={}, amount={}",
                event.paymentId(),
                event.orderId(),
                event.amount()
        );

        validator.validate(event);

        boolean updated =
                processingService.markAsPaid(event);

        if (!updated) {
            log.info(
                    "Order already marked as PAID. orderId={}",
                    event.orderId()
            );

            return;
        }

        log.info(
                "Order marked as PAID. orderId={}",
                event.orderId()
        );
    }
}