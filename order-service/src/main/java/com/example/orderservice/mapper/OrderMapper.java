package com.example.orderservice.mapper;

import com.example.orderservice.dto.OrderItemResponse;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toOrderResponse(Order order);

    OrderItemResponse toOrderItemResponse(OrderItem orderItem);
}