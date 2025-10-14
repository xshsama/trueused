package com.xsh.trueused.mapper;

import com.xsh.trueused.dto.FavoriteDTO;
import com.xsh.trueused.entity.Favorite;

public final class FavoriteMapper {
    private FavoriteMapper() {
    }

    public static FavoriteDTO toDTO(Favorite f) {
        return new FavoriteDTO(
                f.getId(),
                f.getProduct() != null ? f.getProduct().getId() : null,
                f.getUser() != null ? f.getUser().getId() : null,
                f.getNote(),
                f.getCreatedAt());
    }
}
