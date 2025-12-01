package com.xsh.trueused.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.xsh.trueused.enums.ProductCondition;
import com.xsh.trueused.enums.ProductStatus;

public record ProductDTO(
                Long id,
                String title,
                String description,
                BigDecimal price,
                String currency,
                ProductStatus status,
                ProductCondition condition,
                UserDTO seller,
                CategoryDTO category,
                String locationText,
                Double lat,
                Double lng,
                Long viewsCount,
                Long favoritesCount,
                List<ProductImageDTO> images,
                Instant createdAt,
                Instant updatedAt) {
}
