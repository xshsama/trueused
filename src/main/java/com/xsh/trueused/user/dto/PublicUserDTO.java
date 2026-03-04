package com.xsh.trueused.user.dto;

import java.time.Instant;

public record PublicUserDTO(
                Long id,
                String username,
                String nickname,
                String avatarUrl,
                String bio,
                String coverImage,
                String location,
                Instant createdAt,
                Integer sellingCount,
                Integer soldCount) {
}
