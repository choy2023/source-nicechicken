package com.example.nicechicken.product.dto;

import java.math.BigDecimal;

public record ProductOptionResponse(
    Long id,
    Long productId,
    String optionGroup,
    String optionName,
    BigDecimal extraPrice
) {}
