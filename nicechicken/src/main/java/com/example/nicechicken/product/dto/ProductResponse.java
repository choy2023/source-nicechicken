package com.example.nicechicken.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        Long id,
        CategoryResponse category,
        String name,
        BigDecimal basePrice,
        String description,
        String imageUrl,
        Integer stockQuantity,
        List<OptionGroupResponse> optionGroups
) {
    public record OptionGroupResponse(
            String optionGroup,
            List<OptionDetail> options
    ) {}

    public record OptionDetail(
            String optionName,
            BigDecimal extraPrice
    ) {}
}
