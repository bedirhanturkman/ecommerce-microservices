package com.example.inventoryservice.consumer;

import com.example.commonevents.order.OrderCreatedEvent;
import com.example.inventoryservice.service.InventoryService;
import com.example.inventoryservice.validation.OrderCreatedEventValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryConsumer {

    private static final String ORDER_CREATED_TOPIC = "order-created";

    private final InventoryService inventoryService;
    private final OrderCreatedEventValidator eventValidator;

    @KafkaListener(
            topics = ORDER_CREATED_TOPIC,
            containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {

        eventValidator.validate(event);

        log.info(
                "OrderCreatedEvent received. orderId={}, customerId={}, itemCount={}",
                event.orderId(),
                event.customerId(),
                event.items().size()
        );

        event.items().forEach(item -> {
            log.info(
                    "Reducing inventory. orderId={}, productId={}, quantity={}",
                    event.orderId(),
                    item.productId(),
                    item.quantity()
            );

            inventoryService.decreaseStockFromOrder(
                    item.productId(),
                    item.quantity()
            );

            log.info(
                    "Inventory reduced. orderId={}, productId={}, quantity={}",
                    event.orderId(),
                    item.productId(),
                    item.quantity()
            );
        });

        log.info(
                "OrderCreatedEvent processed successfully. orderId={}",
                event.orderId()
        );
    }
}