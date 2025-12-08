package com.xsh.trueused.dto;

import java.time.Instant;

public record BrowsingHistoryDTO(
        Long id,
        ProductDTO product,
        Instant viewedAt) {
}
