package com.example.nicechicken.order.dto;

import com.example.nicechicken.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UserOrderResponse(
    UUID orderId,
    BigDecimal totalAmount,
    OrderStatus status,
    List<OrderItemDetail> items
) {
    public record OrderItemDetail(
        Long productId,
        Integer quantity,
        BigDecimal priceAtOrder,
        Object selectedOptions
    ) {}
}
