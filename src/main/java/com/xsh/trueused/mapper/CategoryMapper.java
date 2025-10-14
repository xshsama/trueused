package com.xsh.trueused.mapper;

import com.xsh.trueused.dto.CategoryDTO;
import com.xsh.trueused.entity.Category;

public final class CategoryMapper {
    private CategoryMapper() {
    }

    public static CategoryDTO toDTO(Category c) {
        return new CategoryDTO(c.getId(), c.getName(), c.getParent() == null ? null : c.getParent().getId(),
                c.getSlug(), c.getPath(), c.getStatus());
    }
}
