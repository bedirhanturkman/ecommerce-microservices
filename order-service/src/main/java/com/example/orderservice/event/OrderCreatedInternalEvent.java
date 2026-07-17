package com.example.orderservice.event;

import com.example.commonevents.order.OrderCreatedEvent;

public record OrderCreatedInternalEvent(
        OrderCreatedEvent event
) {
}