package com.example.paymentservice.consumer;

import com.example.commonevents.inventory.InventoryReservedEvent;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.service.InventoryReservedEventValidator;
import com.example.paymentservice.service.PaymentEventProcessingService;
import com.example.paymentservice.service.PaymentProcessingService;
import com.example.paymentservice.service.model.PaymentInitializationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReservedConsumer {

    private final InventoryReservedEventValidator validator;

    private final PaymentEventProcessingService
            paymentEventProcessingService;

    private final PaymentProcessingService
            paymentProcessingService;

    @KafkaListener(
            topics =
                    "${payment.kafka.topics.inventory-reserved}",
            containerFactory =
                    "inventoryReservedKafkaListenerContainerFactory"
    )
    public void consume(
            InventoryReservedEvent event
    ) {
        log.info(
                "InventoryReservedEvent received. " +
                        "orderId={}, customerId={}, amount={}",
                event.orderId(),
                event.customerId(),
                event.totalAmount()
        );

        validator.validate(event);

        PaymentInitializationResult initialization =
                paymentEventProcessingService
                        .initializePayment(event);

        if (!initialization.created()) {
            log.info(
                    "InventoryReservedEvent already processed. " +
                            "orderId={}",
                    event.orderId()
            );
        } else {
            log.info(
                    "Pending payment created. " +
                            "paymentId={}, orderId={}, amount={}",
                    initialization.paymentId(),
                    initialization.orderId(),
                    event.totalAmount()
            );
        }

        PaymentStatus resultStatus =
                paymentProcessingService
                        .processPendingPayment(
                                initialization.paymentId()
                        );

        log.info(
                "Payment processing completed. " +
                        "paymentId={}, orderId={}, status={}",
                initialization.paymentId(),
                initialization.orderId(),
                resultStatus
        );
    }
}