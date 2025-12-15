package com.xsh.trueused.dto;

import java.time.Instant;
import java.util.List;

public record CommentDTO(
                Long id,
                Long productId,
                Long targetUserId,
                PublicUserDTO user,
                String content,
                Instant createdAt,
                List<CommentDTO> replies) {
}
