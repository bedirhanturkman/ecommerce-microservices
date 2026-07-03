package com.example.orderservice.service;

import com.example.orderservice.dto.CreateOrderItemRequest;
import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderItemResponse;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.ProductResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestClient restClient;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8083")
                .build();
    }

    public OrderResponse createOrder(CreateOrderRequest request) {

        Order order = Order.builder()
                .customerId(request.customerId())
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .totalPrice(BigDecimal.ZERO)
                .build();

        List<OrderItem> orderItems = request.items()
                .stream()
                .map(itemRequest -> createOrderItem(order, itemRequest))
                .toList();

        BigDecimal totalPrice = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.getItems().addAll(orderItems);
        order.setTotalPrice(totalPrice);

        Order savedOrder = orderRepository.save(order);

        return toOrderResponse(savedOrder);
    }

    private OrderItem createOrderItem(
            Order order,
            CreateOrderItemRequest itemRequest
    ) {
        ProductResponse product = restClient.get()
                .uri("/api/v1/products/{id}", itemRequest.productId())
                .retrieve()
                .body(ProductResponse.class);

        if (product == null) {
            throw new RuntimeException("Product not found");
        }

        BigDecimal totalPrice = product.price().multiply(
                BigDecimal.valueOf(itemRequest.quantity())
        );

        return OrderItem.builder()
                .order(order)
                .productId(product.id())
                .productName(product.name())
                .quantity(itemRequest.quantity())
                .unitPrice(product.price())
                .totalPrice(totalPrice)
                .build();
    }

    private OrderResponse toOrderResponse(Order order) {

        List<OrderItemResponse> itemResponses = order.getItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getTotalPrice()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt(),
                itemResponses
        );
    }
}