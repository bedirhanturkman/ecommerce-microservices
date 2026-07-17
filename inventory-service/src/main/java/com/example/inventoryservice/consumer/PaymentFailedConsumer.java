package com.example.inventoryservice.consumer;

import com.example.commonevents.payment.PaymentFailedEvent;
import com.example.inventoryservice.service.PaymentResultEventValidator;
import com.example.inventoryservice.service.PaymentResultProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFailedConsumer {

    private final PaymentResultEventValidator validator;

    private final PaymentResultProcessingService
            processingService;

    @KafkaListener(
            topics =
                    "${inventory.kafka.topics.payment-failed}",
            containerFactory =
                    "paymentFailedKafkaListenerContainerFactory"
    )
    public void consume(
            PaymentFailedEvent event
    ) {
        log.info(
                "PaymentFailedEvent received. " +
                        "paymentId={}, orderId={}, failureCode={}",
                event.paymentId(),
                event.orderId(),
                event.failureCode()
        );

        validator.validate(event);

        boolean released =
                processingService
                        .processFailedPayment(event);

        if (!released) {
            log.info(
                    "Inventory reservation already released. " +
                            "orderId={}",
                    event.orderId()
            );

            return;
        }

        log.info(
                "Inventory reservation released. orderId={}",
                event.orderId()
        );
    }
}