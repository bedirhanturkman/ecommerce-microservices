package com.example.orderservice.service;

import com.example.commonevents.order.OrderCreatedEvent;
import com.example.orderservice.client.ProductClient;
import com.example.orderservice.dto.CreateOrderItemRequest;
import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.ProductResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.mapper.OrderEventMapper;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.outbox.OrderOutboxService;
import com.example.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;
    private final OrderEventMapper orderEventMapper;
    private final OrderOutboxService orderOutboxService;

    public OrderService(
            OrderRepository orderRepository,
            OrderMapper orderMapper,
            ProductClient productClient,
            OrderEventMapper orderEventMapper,
            OrderOutboxService orderOutboxService
    ) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.productClient = productClient;
        this.orderEventMapper = orderEventMapper;
        this.orderOutboxService =
                orderOutboxService;
    }

    @Transactional
    public OrderResponse createOrder(
            CreateOrderRequest request,
            String authorizationHeader
    ) {
        Order order = Order.builder()
                .customerId(
                        request.customerId()
                )
                .status(
                        OrderStatus.CREATED
                )
                .createdAt(
                        LocalDateTime.now()
                )
                .totalPrice(
                        BigDecimal.ZERO
                )
                .build();

        List<OrderItem> orderItems =
                request.items()
                        .stream()
                        .map(
                                itemRequest ->
                                        createOrderItem(
                                                order,
                                                itemRequest,
                                                authorizationHeader
                                        )
                        )
                        .toList();

        BigDecimal totalPrice =
                orderItems.stream()
                        .map(
                                OrderItem::getTotalPrice
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        order.getItems().addAll(
                orderItems
        );

        order.setTotalPrice(
                totalPrice
        );

        Order savedOrder =
                orderRepository.save(order);

        OrderCreatedEvent orderCreatedEvent =
                orderEventMapper
                        .toOrderCreatedEvent(
                                savedOrder
                        );

        /*
         * Order ve Outbox kaydı aynı PostgreSQL
         * transaction içerisinde oluşturulur.
         */
        orderOutboxService
                .saveOrderCreatedEvent(
                        orderCreatedEvent
                );

        return orderMapper.toOrderResponse(
                savedOrder
        );
    }

    private OrderItem createOrderItem(
            Order order,
            CreateOrderItemRequest itemRequest,
            String authorizationHeader
    ) {
        ProductResponse product =
                productClient.getProductById(
                        itemRequest.productId(),
                        authorizationHeader
                );

        BigDecimal totalPrice =
                product.price().multiply(
                        BigDecimal.valueOf(
                                itemRequest.quantity()
                        )
                );

        return OrderItem.builder()
                .order(order)
                .productId(product.id())
                .productName(product.name())
                .quantity(
                        itemRequest.quantity()
                )
                .unitPrice(product.price())
                .totalPrice(totalPrice)
                .build();
    }
}