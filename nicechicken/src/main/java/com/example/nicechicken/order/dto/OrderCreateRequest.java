package com.example.nicechicken.order.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record OrderCreateRequest(
        @NotBlank(message = "Customer name is required.")
        String customerName,

        @NotBlank(message = "Phone number is required.")
        String customerPhone,

        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email format.")
        String customerEmail,

        @NotNull(message = "Payment amount is required.")
        @Min(value = 0, message = "Payment amount must be 0 or more.")
        BigDecimal clientTotalAmount,

        @NotEmpty(message = "At least one order item is required.")
        List<OrderItemRequest> items
) {
    public record OrderItemRequest(
            @NotNull(message = "Product ID is required.")
            Long productId,

            @NotNull(message = "Quantity is required.")
            @Min(value = 1, message = "Quantity must be at least 1.")
            Integer quantity,

            @NotNull(message = "Selected options list is required.")
            List<SelectedOptionRequest> selectedOptions
    ) {}

    public record SelectedOptionRequest(
            @NotBlank(message = "Option group name is required.")
            String optionGroup,

            @NotBlank(message = "Option name is required.")
            String optionName
    ) {}
}
