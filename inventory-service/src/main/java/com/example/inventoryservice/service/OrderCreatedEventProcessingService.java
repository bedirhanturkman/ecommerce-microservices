package com.example.inventoryservice.service;

import com.example.commonevents.order.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreatedEventProcessingService {

    private final InventoryService inventoryService;
    private final ProcessedEventService processedEventService;

    @Transactional
    public boolean process(OrderCreatedEvent event) {

        if (processedEventService.isProcessed(event.orderId())) {
            return false;
        }

        for (var item : event.items()) {
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
        }

        processedEventService.markAsProcessed(event.orderId());

        return true;
    }
}