package com.example.nicechicken.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        Long categoryId,
        String name,
        BigDecimal basePrice,
        String description,
        String imageUrl,
        Integer stockQuantity,
        List<OptionGroupRequest> optionGroups
) {
    public record OptionGroupRequest(
            String optionGroup,
            List<OptionRequest> options
    ) {}

    public record OptionRequest(
            String optionName,
            BigDecimal extraPrice
    ) {}
}
