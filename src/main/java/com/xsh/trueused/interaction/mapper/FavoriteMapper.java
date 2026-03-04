package com.xsh.trueused.interaction.mapper;

import com.xsh.trueused.interaction.dto.FavoriteDTO;
import com.xsh.trueused.entity.Favorite;
import com.xsh.trueused.product.mapper.ProductMapper;

public final class FavoriteMapper {
    private FavoriteMapper() {
    }

    public static FavoriteDTO toDTO(Favorite f) {
        if (f == null) {
            return null;
        }
        return new FavoriteDTO(
                f.getId(),
                f.getProduct() != null ? f.getProduct().getId() : null,
                f.getUser() != null ? f.getUser().getId() : null,
                f.getNote(),
                f.getCreatedAt(),
                f.getProduct() != null ? ProductMapper.enrich(ProductMapper.toDTO(f.getProduct())) : null);
    }
}
