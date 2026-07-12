package com.example.orderservice.producer;

import com.example.commonevents.order.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProducer {

    private static final String ORDER_CREATED_TOPIC = "order-created";

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void publishOrderCreatedEvent(OrderCreatedEvent event) {
        kafkaTemplate.send(
                ORDER_CREATED_TOPIC,
                String.valueOf(event.orderId()),
                event
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(
                        "OrderCreatedEvent could not be published. orderId={}",
                        event.orderId(),
                        exception
                );
                return;
            }

            log.info(
                    "OrderCreatedEvent published. orderId={}, partition={}, offset={}",
                    event.orderId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });
    }
}