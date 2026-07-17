package com.example.orderservice.consumer;

import com.example.commonevents.inventory.InventoryReservationFailedEvent;
import com.example.orderservice.service.OrderResultEventValidator;
import com.example.orderservice.service.OrderResultProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReservationFailedConsumer {

    private final OrderResultEventValidator validator;
    private final OrderResultProcessingService
            processingService;

    @KafkaListener(
            topics =
                    "${order.kafka.topics.inventory-reservation-failed}",
            containerFactory =
                    "inventoryReservationFailedKafkaListenerContainerFactory"
    )
    public void consume(
            InventoryReservationFailedEvent event
    ) {
        log.info(
                "InventoryReservationFailedEvent received by "
                        + "Order Service. orderId={}, errorCode={}",
                event.orderId(),
                event.errorCode()
        );

        validator.validate(event);

        boolean updated =
                processingService
                        .markAsInventoryFailed(event);

        if (!updated) {
            log.info(
                    "Order already marked as INVENTORY_FAILED. "
                            + "orderId={}",
                    event.orderId()
            );

            return;
        }

        log.info(
                "Order marked as INVENTORY_FAILED. orderId={}",
                event.orderId()
        );
    }
}