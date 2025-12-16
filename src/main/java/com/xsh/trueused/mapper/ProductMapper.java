package com.xsh.trueused.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.xsh.trueused.dto.CategoryDTO;
import com.xsh.trueused.dto.ProductDTO;
import com.xsh.trueused.dto.ProductImageDTO;
import com.xsh.trueused.dto.UserDTO;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.entity.ProductImage;
import com.xsh.trueused.util.CloudinaryUrlHelper;

public final class ProductMapper {
        private ProductMapper() {
        }

        public static ProductDTO toDTO(Product p) {
                List<ProductImageDTO> images = p.getImages() == null ? List.of()
                                : p.getImages().stream()
                                                .sorted(Comparator.comparing(ProductImage::getSort))
                                                .map(img -> new ProductImageDTO(
                                                                img.getId(),
                                                                img.getImageKey(),
                                                                null,
                                                                null,
                                                                null,
                                                                null,
                                                                img.getSort(),
                                                                img.getIsCover()))
                                                .collect(Collectors.toList());

                UserDTO seller = p.getSeller() != null ? UserMapper.toDTO(p.getSeller()) : null;
                CategoryDTO category = p.getCategory() != null ? CategoryMapper.toDTO(p.getCategory()) : null;

                return new ProductDTO(
                                p.getId(),
                                p.getTitle(),
                                p.getDescription(),
                                p.getPrice(),
                                p.getOriginalPrice(),
                                p.getHeatScore(),
                                p.getCurrency(),
                                p.getStatus(),
                                p.getCondition(),
                                p.getTradeModel(),
                                seller,
                                category,
                                p.getLocationText(),
                                p.getLat(),
                                p.getLng(),
                                p.getViewsCount(),
                                p.getFavoritesCount(),
                                images,
                                p.getCreatedAt(),
                                p.getUpdatedAt());
        }

        public static ProductDTO enrich(ProductDTO dto) {
                if (dto == null) {
                        return null;
                }
                List<ProductImageDTO> enrichedImages = dto.images() == null ? List.of()
                                : dto.images().stream()
                                                .map(img -> new ProductImageDTO(
                                                                img.id(),
                                                                img.imageKey(),
                                                                CloudinaryUrlHelper.getUrl(img.imageKey()),
                                                                CloudinaryUrlHelper.getThumbnailUrl(img.imageKey()),
                                                                CloudinaryUrlHelper.getDetailUrl(img.imageKey()),
                                                                CloudinaryUrlHelper.getBlurUrl(img.imageKey()),
                                                                img.sort(),
                                                                img.isCover()))
                                                .collect(Collectors.toList());

                return new ProductDTO(
                                dto.id(),
                                dto.title(),
                                dto.description(),
                                dto.price(),
                                dto.originalPrice(),
                                dto.heatScore(),
                                dto.currency(),
                                dto.status(),
                                dto.condition(),
                                dto.tradeModel(),
                                dto.seller(),
                                dto.category(),
                                dto.locationText(),
                                dto.lat(),
                                dto.lng(),
                                dto.viewsCount(),
                                dto.favoritesCount(),
                                enrichedImages,
                                dto.createdAt(),
                                dto.updatedAt());
        }
}
