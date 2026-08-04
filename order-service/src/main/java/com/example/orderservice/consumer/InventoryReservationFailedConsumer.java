package com.example.orderservice.consumer;

import com.example.commonevents.inventory.InventoryReservationFailedEvent;
import com.example.orderservice.metrics.OrderMetrics;
import com.example.orderservice.service.OrderResultEventValidator;
import com.example.orderservice.service.OrderResultProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.messaging.handler.annotation.Header;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReservationFailedConsumer {

    private final OrderResultEventValidator validator;
    private final OrderResultProcessingService
            processingService;
    private final OrderMetrics orderMetrics;

    @KafkaListener(
            topics =
                    "${order.kafka.topics.inventory-reservation-failed}",
            containerFactory =
                    "inventoryReservationFailedKafkaListenerContainerFactory"
    )
    public void consume(
            InventoryReservationFailedEvent event,
            @Header("outbox-event-id") byte[] eventId
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
                        .markAsInventoryFailed(
                                UUID.fromString(new String(eventId, StandardCharsets.UTF_8)),
                                event);

        if (!updated) {
            log.info(
                    "Order already marked as INVENTORY_FAILED. "
                            + "orderId={}",
                    event.orderId()
            );

            return;
        }

        /*
         * Aynı event tekrar gelirse updated=false olacağı
         * için sayaç ikinci kez artırılmaz.
         */
        orderMetrics.incrementInventoryFailedOrders();

        log.info(
                "Order marked as INVENTORY_FAILED. orderId={}",
                event.orderId()
        );
    }
}
