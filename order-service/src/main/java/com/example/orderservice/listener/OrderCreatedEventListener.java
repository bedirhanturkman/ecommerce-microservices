package com.example.orderservice.listener;

import com.example.orderservice.event.OrderCreatedInternalEvent;
import com.example.orderservice.producer.OrderProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventListener {

    private final OrderProducer orderProducer;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
            OrderCreatedInternalEvent internalEvent
    ) {
        log.info(
                "Order transaction committed. " +
                        "OrderCreatedEvent will be published. orderId={}",
                internalEvent.event().orderId()
        );

        orderProducer.publishOrderCreatedEvent(
                internalEvent.event()
        );
    }
}