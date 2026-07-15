package com.example.inventoryservice.producer;

import com.example.commonevents.inventory.InventoryReservationFailedEvent;
import com.example.commonevents.inventory.InventoryReservedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String reservedTopic;
    private final String reservationFailedTopic;

    public InventoryEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${inventory.kafka.topics.reserved}")
            String reservedTopic,
            @Value("${inventory.kafka.topics.reservation-failed}")
            String reservationFailedTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.reservedTopic = reservedTopic;
        this.reservationFailedTopic =
                reservationFailedTopic;
    }

    public void publishInventoryReserved(
            InventoryReservedEvent event
    ) {
        kafkaTemplate.send(
                reservedTopic,
                String.valueOf(event.orderId()),
                event
        ).whenComplete((result, exception) -> {

            if (exception != null) {
                log.error(
                        "InventoryReservedEvent could not be published. " +
                                "orderId={}",
                        event.orderId(),
                        exception
                );
                return;
            }

            log.info(
                    "InventoryReservedEvent published. " +
                            "orderId={}, partition={}, offset={}",
                    event.orderId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });
    }

    public void publishReservationFailed(
            InventoryReservationFailedEvent event
    ) {
        kafkaTemplate.send(
                reservationFailedTopic,
                String.valueOf(event.orderId()),
                event
        ).whenComplete((result, exception) -> {

            if (exception != null) {
                log.error(
                        "InventoryReservationFailedEvent could not be published. " +
                                "orderId={}, errorCode={}",
                        event.orderId(),
                        event.errorCode(),
                        exception
                );
                return;
            }

            log.info(
                    "InventoryReservationFailedEvent published. " +
                            "orderId={}, errorCode={}, partition={}, offset={}",
                    event.orderId(),
                    event.errorCode(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });
    }
}