package com.xsh.trueused.interaction.dto;

import java.time.Instant;

import com.xsh.trueused.product.dto.ProductDTO;

public record FavoriteDTO(Long id, Long productId, Long userId, String note, Instant createdAt, ProductDTO product) {
}
