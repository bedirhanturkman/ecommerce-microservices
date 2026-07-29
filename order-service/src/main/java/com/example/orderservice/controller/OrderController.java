package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.security.OrderAccessContext;
import com.example.orderservice.security.OrderAccessContextResolver;
import com.example.orderservice.security.PermissionConstants;
import com.example.orderservice.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderAccessContextResolver accessContextResolver;

    public OrderController(
            OrderService orderService,
            OrderAccessContextResolver accessContextResolver
    ) {
        this.orderService = orderService;
        this.accessContextResolver = accessContextResolver;
    }

    @PostMapping
    @PreAuthorize(PermissionConstants.HAS_ROLE_USER_OR_ADMIN)
    public OrderResponse createOrder(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {
        OrderAccessContext accessContext =
                accessContextResolver.resolve(authentication);

        return orderService.createOrder(
                request,
                authorizationHeader,
                accessContext
        );
    }

    @GetMapping("/me")
    @PreAuthorize(PermissionConstants.HAS_ROLE_USER)
    public Page<OrderResponse> findCurrentCustomerOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        OrderAccessContext accessContext =
                accessContextResolver.resolve(authentication);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return orderService.findCurrentCustomerOrders(
                accessContext.customerId(),
                pageRequest
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(PermissionConstants.HAS_ROLE_USER_OR_ADMIN)
    public OrderResponse findById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        OrderAccessContext accessContext =
                accessContextResolver.resolve(authentication);

        return orderService.findOrderById(id, accessContext);
    }

}
