package com.xsh.trueused.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCommentRequest(
        @NotNull Long productId,
        @NotBlank String content,
        Long parentId) {
}
