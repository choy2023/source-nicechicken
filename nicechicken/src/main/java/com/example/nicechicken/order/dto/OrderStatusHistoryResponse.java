package com.example.nicechicken.order.dto;

import com.example.nicechicken.order.entity.OrderStatus;

import java.time.LocalDateTime;

public record OrderStatusHistoryResponse(
        OrderStatus oldStatus,
        OrderStatus newStatus,
        LocalDateTime changedAt,
        String changedBy
) {}
