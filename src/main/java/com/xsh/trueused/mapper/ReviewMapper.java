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
    @Mapping(source = "product.image", target = "productImage") // Assuming product has image field or logic needed
    @Mapping(source = "buyer.id", target = "buyerId")
    @Mapping(source = "buyer.username", target = "buyerName")
    // @Mapping(source = "buyer.avatar", target = "buyerAvatar") // Assuming user
    // has avatar
    ReviewDTO toDTO(Review review);
}
