package com.example.paymentservice.consumer;

import com.example.commonevents.inventory.InventoryReservedEvent;
import com.example.paymentservice.service.InventoryReservedEventValidator;
import com.example.paymentservice.service.PaymentEventProcessingService;
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

        boolean processed =
                paymentEventProcessingService.process(
                        event
                );

        if (!processed) {
            log.info(
                    "InventoryReservedEvent already processed. " +
                            "orderId={}",
                    event.orderId()
            );

            return;
        }

        log.info(
                "Pending payment created. orderId={}, amount={}",
                event.orderId(),
                event.totalAmount()
        );
    }
}