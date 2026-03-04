package com.xsh.trueused.product.dto;

import java.math.BigDecimal;
import java.util.List;

import com.xsh.trueused.enums.ProductCondition;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.enums.ProductTradeModel;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
                @Size(max = 120) String title,
                String description,
                @DecimalMin("0.0") BigDecimal price,
                @DecimalMin("0.0") BigDecimal originalPrice,
                @Size(max = 3) String currency,
                ProductStatus status,
                ProductCondition condition,
                Long categoryId,
                @Size(max = 100) String locationText,
                @Size(max = 20) String shippingPayer,
                @Size(max = 50) String tradeTypes,
                ProductTradeModel tradeModel,
                Double lat,
                Double lng,
                List<@Size(max = 255) String> imageKeys) {
}
