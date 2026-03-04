package com.xsh.trueused.interaction.dto;

import java.time.Instant;

import com.xsh.trueused.product.dto.ProductDTO;

public record BrowsingHistoryDTO(
        Long id,
        ProductDTO product,
        Instant viewedAt) {
}
