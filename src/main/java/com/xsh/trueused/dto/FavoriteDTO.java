package com.xsh.trueused.dto;

import java.time.Instant;

public record FavoriteDTO(Long id, Long productId, Long userId, String note, Instant createdAt) {
}
