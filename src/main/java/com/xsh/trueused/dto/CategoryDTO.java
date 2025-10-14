package com.xsh.trueused.dto;

public record CategoryDTO(Long id, String name, Long parentId, String slug, String path, String status) {
}
