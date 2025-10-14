package com.xsh.trueused.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.xsh.trueused.dto.ProductDTO;
import com.xsh.trueused.dto.ProductImageDTO;
import com.xsh.trueused.entity.Category;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.entity.ProductImage;

public final class ProductMapper {
    private ProductMapper() {
    }

    public static ProductDTO toDTO(Product p) {
        List<ProductImageDTO> images = p.getImages() == null ? List.of()
                : p.getImages().stream()
                        .sorted(Comparator.comparing(ProductImage::getSort))
                        .map(img -> new ProductImageDTO(img.getId(), img.getUrl(), img.getSort(), img.getIsCover()))
                        .collect(Collectors.toList());
        Category cat = p.getCategory();
        return new ProductDTO(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getPrice(),
                p.getCurrency(),
                p.getStatus(),
                p.getCondition(),
                p.getSeller() != null ? p.getSeller().getId() : null,
                cat != null ? cat.getId() : null,
                p.getLocationText(),
                p.getLat(),
                p.getLng(),
                p.getViewsCount(),
                p.getFavoritesCount(),
                images,
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
