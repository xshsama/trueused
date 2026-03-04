package com.xsh.trueused.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.xsh.trueused.category.dto.CategoryDTO;
import com.xsh.trueused.user.dto.UserDTO;
import com.xsh.trueused.enums.ProductCondition;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.enums.ProductTradeModel;

public record ProductDTO(
                Long id,
                String title,
                String description,
                BigDecimal price,
                BigDecimal originalPrice,
                Double heatScore,
                String currency,
                ProductStatus status,
                ProductCondition condition,
                ProductTradeModel tradeModel,
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
