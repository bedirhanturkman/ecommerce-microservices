package com.example.orderservice.mapper;

import com.example.commonevents.order.OrderCreatedEvent;
import com.example.commonevents.order.OrderCreatedItemEvent;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderEventMapper {

    public OrderCreatedEvent toOrderCreatedEvent(Order order) {
        List<OrderCreatedItemEvent> items = order.getItems()
                .stream()
                .map(this::toOrderCreatedItemEvent)
                .toList();

        return new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getTotalPrice(),
                order.getStatus().name(),
                order.getCreatedAt(),
                items
        );
    }

    private OrderCreatedItemEvent toOrderCreatedItemEvent(OrderItem item) {
        return new OrderCreatedItemEvent(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }
}