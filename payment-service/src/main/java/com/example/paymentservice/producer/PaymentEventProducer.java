package com.example.paymentservice.producer;

import com.example.commonevents.payment.PaymentFailedEvent;
import com.example.commonevents.payment.PaymentSucceededEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String paymentSucceededTopic;
    private final String paymentFailedTopic;

    public PaymentEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,

            @Value("${payment.kafka.topics.succeeded}")
            String paymentSucceededTopic,

            @Value("${payment.kafka.topics.failed}")
            String paymentFailedTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.paymentSucceededTopic =
                paymentSucceededTopic;

        this.paymentFailedTopic =
                paymentFailedTopic;
    }

    public void publishPaymentSucceeded(
            PaymentSucceededEvent event
    ) {
        kafkaTemplate.send(
                paymentSucceededTopic,
                String.valueOf(event.orderId()),
                event
        ).whenComplete((result, exception) -> {

            if (exception != null) {
                log.error(
                        "PaymentSucceededEvent could not be published. " +
                                "paymentId={}, orderId={}",
                        event.paymentId(),
                        event.orderId(),
                        exception
                );

                return;
            }

            log.info(
                    "PaymentSucceededEvent published. " +
                            "paymentId={}, orderId={}, partition={}, offset={}",
                    event.paymentId(),
                    event.orderId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });
    }

    public void publishPaymentFailed(
            PaymentFailedEvent event
    ) {
        kafkaTemplate.send(
                paymentFailedTopic,
                String.valueOf(event.orderId()),
                event
        ).whenComplete((result, exception) -> {

            if (exception != null) {
                log.error(
                        "PaymentFailedEvent could not be published. " +
                                "paymentId={}, orderId={}, failureCode={}",
                        event.paymentId(),
                        event.orderId(),
                        event.failureCode(),
                        exception
                );

                return;
            }

            log.info(
                    "PaymentFailedEvent published. " +
                            "paymentId={}, orderId={}, failureCode={}, " +
                            "partition={}, offset={}",
                    event.paymentId(),
                    event.orderId(),
                    event.failureCode(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });
    }
}