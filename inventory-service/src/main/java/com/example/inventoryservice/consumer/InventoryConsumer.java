package com.example.inventoryservice.consumer;

import com.example.commonevents.order.OrderCreatedEvent;
import com.example.inventoryservice.service.OrderCreatedEventProcessingService;
import com.example.inventoryservice.validation.OrderCreatedEventValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryConsumer {

    private final OrderCreatedEventProcessingService
            processingService;

    private final OrderCreatedEventValidator
            eventValidator;

    @KafkaListener(
            topics =
                    "${inventory.kafka.topics.order-created}",
            containerFactory =
                    "orderCreatedKafkaListenerContainerFactory"
    )
    public void consumeOrderCreatedEvent(
            OrderCreatedEvent event
    ) {
        eventValidator.validate(event);

        log.info(
                "OrderCreatedEvent received by Inventory Service. "
                        + "orderId={}, itemCount={}",
                event.orderId(),
                event.items().size()
        );

        boolean processed =
                processingService.process(event);

        if (!processed) {
            log.info(
                    "OrderCreatedEvent already processed. "
                            + "orderId={}",
                    event.orderId()
            );

            return;
        }

        log.info(
                "Inventory reservation transaction completed. "
                        + "orderId={}",
                event.orderId()
        );
    }
}