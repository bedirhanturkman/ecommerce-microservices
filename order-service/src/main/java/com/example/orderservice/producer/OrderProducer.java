package com.example.orderservice.producer;

import com.example.commonevents.order.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderProducer {

    private final KafkaTemplate<String, Object>
            orderKafkaTemplate;

    private final String orderCreatedTopic;

    public OrderProducer(
            @Qualifier("orderKafkaTemplate")
            KafkaTemplate<String, Object>
                    orderKafkaTemplate,

            @Value("${order.kafka.topics.order-created}")
            String orderCreatedTopic
    ) {
        this.orderKafkaTemplate =
                orderKafkaTemplate;

        this.orderCreatedTopic =
                orderCreatedTopic;
    }

    public void publishOrderCreatedEvent(
            OrderCreatedEvent event
    ) {
        orderKafkaTemplate.send(
                orderCreatedTopic,
                String.valueOf(event.orderId()),
                event
        ).whenComplete((result, exception) -> {

            if (exception != null) {
                log.error(
                        "OrderCreatedEvent could not be published. "
                                + "orderId={}",
                        event.orderId(),
                        exception
                );

                return;
            }

            log.info(
                    "OrderCreatedEvent published. "
                            + "orderId={}, partition={}, offset={}",
                    event.orderId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });
    }
}