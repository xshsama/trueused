package com.xsh.trueused.dto;

import java.math.BigDecimal;
import java.util.List;

import com.xsh.trueused.enums.ProductCondition;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank String description,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        @Size(max = 3) String currency,
        ProductCondition condition,
        Long categoryId,
        @Size(max = 100) String locationText,
        Double lat,
        Double lng,
        @Size(max = 9) List<@Size(max = 255) String> imageUrls) {
}
