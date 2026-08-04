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
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.mapper.OrderEventMapper;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.metrics.OrderMetrics;
import com.example.orderservice.outbox.OrderOutboxService;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.security.OrderAccessContext;
import com.example.orderservice.saga.OrderSagaProjectionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;
    private final OrderEventMapper orderEventMapper;
    private final OrderOutboxService orderOutboxService;
    private final OrderMetrics orderMetrics;
    private final OrderSagaProjectionService sagaProjectionService;

    public OrderService(
            OrderRepository orderRepository,
            OrderMapper orderMapper,
            ProductClient productClient,
            OrderEventMapper orderEventMapper,
            OrderOutboxService orderOutboxService,
            OrderMetrics orderMetrics,
            OrderSagaProjectionService sagaProjectionService
    ) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.productClient = productClient;
        this.orderEventMapper = orderEventMapper;
        this.orderOutboxService =
                orderOutboxService;
        this.orderMetrics = orderMetrics;
        this.sagaProjectionService = sagaProjectionService;
    }

    @Transactional
    public OrderResponse createOrder(
            CreateOrderRequest request,
            String authorizationHeader,
            OrderAccessContext accessContext
    ) {
        return orderMetrics.recordOrderCreation(
                () -> createOrderAndOutboxEvent(
                        request,
                        authorizationHeader,
                        accessContext.customerId()
                )
        );
    }

    private OrderResponse createOrderAndOutboxEvent(
            CreateOrderRequest request,
            String authorizationHeader,
            Long customerId
    ) {
        Order order = Order.builder()
                .customerId(
                        customerId
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

        sagaProjectionService.initialize(savedOrder.getId());

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

        /*
         * Sayaç ancak mevcut transaction başarıyla
         * commit edilirse artırılır.
         */
        orderMetrics
                .incrementCreatedOrdersAfterCommit();

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

    @Transactional(readOnly = true)
    public OrderResponse findOrderById(
            Long id,
            OrderAccessContext accessContext
    ) {
        Order order = findAccessibleOrder(id, accessContext)
                .orElseThrow(
                        () -> new OrderNotFoundException(id)
                );

        return orderMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findCurrentCustomerOrders(
            Long customerId,
            Pageable pageable
    ) {
        return orderRepository
                .findByCustomerId(customerId, pageable)
                .map(orderMapper::toOrderResponse);
    }

    private Optional<Order> findAccessibleOrder(
            Long id,
            OrderAccessContext accessContext
    ) {
        if (accessContext.admin()) {
            return orderRepository.findById(id);
        }

        return orderRepository.findByIdAndCustomerId(
                id,
                accessContext.customerId()
        );
    }
}
