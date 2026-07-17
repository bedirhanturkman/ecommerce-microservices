package com.example.inventoryservice.consumer;

import com.example.commonevents.payment.PaymentSucceededEvent;
import com.example.inventoryservice.service.PaymentResultEventValidator;
import com.example.inventoryservice.service.PaymentResultProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSucceededConsumer {

    private final PaymentResultEventValidator validator;

    private final PaymentResultProcessingService
            processingService;

    @KafkaListener(
            topics =
                    "${inventory.kafka.topics.payment-succeeded}",
            containerFactory =
                    "paymentSucceededKafkaListenerContainerFactory"
    )
    public void consume(
            PaymentSucceededEvent event
    ) {
        log.info(
                "PaymentSucceededEvent received. " +
                        "paymentId={}, orderId={}, amount={}",
                event.paymentId(),
                event.orderId(),
                event.amount()
        );

        validator.validate(event);

        boolean confirmed =
                processingService
                        .processSucceededPayment(event);

        if (!confirmed) {
            log.info(
                    "Inventory reservation already confirmed. " +
                            "orderId={}",
                    event.orderId()
            );

            return;
        }

        log.info(
                "Inventory reservation confirmed. orderId={}",
                event.orderId()
        );
    }
}