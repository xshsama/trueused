package com.xsh.trueused.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
                Long productId,
                Long targetUserId,
                @NotBlank String content,
                Long parentId) {
}
