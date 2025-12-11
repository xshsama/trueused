package com.xsh.trueused.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConsignmentCreateRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Expected price is required")
    private BigDecimal expectedPrice;

    @NotBlank(message = "Shipping method is required")
    private String shippingMethod;

    private String trackingNoInbound;

    @NotNull(message = "Category ID is required")
    private Long categoryId;
}
