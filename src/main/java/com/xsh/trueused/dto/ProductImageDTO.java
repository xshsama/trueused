package com.xsh.trueused.dto;

public record ProductImageDTO(
                Long id,
                String imageKey,
                String url,
                String thumbnailUrl,
                String detailUrl,
                String blurUrl,
                Integer sort,
                Boolean isCover) {
}
