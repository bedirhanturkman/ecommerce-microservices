package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.security.PermissionConstants;
import com.example.orderservice.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize(PermissionConstants.HAS_ROLE_USER_OR_ADMIN)
    public OrderResponse createOrder(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody CreateOrderRequest request
    ) {
        return orderService.createOrder(request, authorizationHeader);
    }
}