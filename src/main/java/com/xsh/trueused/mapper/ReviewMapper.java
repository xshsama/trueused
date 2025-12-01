package com.xsh.trueused.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.xsh.trueused.dto.ReviewDTO;
import com.xsh.trueused.entity.Review;

@Mapper
public interface ReviewMapper {
    ReviewMapper INSTANCE = Mappers.getMapper(ReviewMapper.class);

    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.title", target = "productTitle")
    @Mapping(target = "productImage", expression = "java(getProductImage(review))")
    @Mapping(source = "buyer.id", target = "buyerId")
    @Mapping(source = "buyer.username", target = "buyerName")
    @Mapping(source = "buyer.avatarUrl", target = "buyerAvatar")
    ReviewDTO toDTO(Review review);

    default String getProductImage(Review review) {
        if (review.getProduct() != null && review.getProduct().getImages() != null
                && !review.getProduct().getImages().isEmpty()) {
            return review.getProduct().getImages().get(0).getUrl();
        }
        return null;
    }
}
