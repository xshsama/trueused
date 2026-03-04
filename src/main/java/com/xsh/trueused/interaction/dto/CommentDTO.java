package com.xsh.trueused.interaction.dto;

import java.time.Instant;
import java.util.List;

import com.xsh.trueused.user.dto.PublicUserDTO;

public record CommentDTO(
                Long id,
                Long productId,
                Long targetUserId,
                PublicUserDTO user,
                String content,
                Instant createdAt,
                List<CommentDTO> replies) {
}
