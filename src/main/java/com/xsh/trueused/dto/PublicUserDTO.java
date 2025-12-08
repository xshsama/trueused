package com.xsh.trueused.dto;

import java.time.Instant;

public record PublicUserDTO(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        Instant createdAt,
        Integer sellingCount,
        Integer soldCount) {
}
