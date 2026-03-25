package com.example.nicechicken.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    String customerName,
    BigDecimal totalAmount,
    String status
) {}