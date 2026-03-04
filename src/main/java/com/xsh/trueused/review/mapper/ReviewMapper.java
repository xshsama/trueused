package com.xsh.trueused.review.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.xsh.trueused.review.dto.ReviewDTO;
import com.xsh.trueused.entity.Review;
import com.xsh.trueused.entity.ReviewImage;
import com.xsh.trueused.util.CloudinaryUrlHelper;

public final class ReviewMapper {
    public static final ReviewMapper INSTANCE = new ReviewMapper();

    private ReviewMapper() {
    }

    public ReviewDTO toDTO(Review review) {
        if (review == null) {
            return null;
        }

        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setOrderId(review.getOrder() != null ? review.getOrder().getId() : null);
        dto.setProductId(review.getProduct() != null ? review.getProduct().getId() : null);
        dto.setProductTitle(review.getProduct() != null ? review.getProduct().getTitle() : null);
        dto.setPrice(review.getProduct() != null ? review.getProduct().getPrice() : null);
        dto.setProductImage(getProductImage(review));
        dto.setBuyerId(review.getBuyer() != null ? review.getBuyer().getId() : null);
        dto.setBuyerName(review.getBuyer() != null ? review.getBuyer().getUsername() : null);
        dto.setBuyerAvatar(review.getBuyer() != null ? review.getBuyer().getAvatarUrl() : null);
        dto.setRating(review.getRating());
        dto.setContent(review.getContent());
        dto.setIsAnonymous(review.getIsAnonymous());
        dto.setSellerReply(review.getSellerReply());
        dto.setSellerReplyAt(review.getSellerReplyAt());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setImages(getReviewImages(review));
        return dto;
    }

    private String getProductImage(Review review) {
        if (review.getProduct() != null && review.getProduct().getImages() != null
                && !review.getProduct().getImages().isEmpty()) {
            return CloudinaryUrlHelper.getUrl(review.getProduct().getImages().get(0).getImageKey());
        }
        return null;
    }

    private List<String> getReviewImages(Review review) {
        if (review.getImages() != null) {
            return review.getImages().stream()
                    .map(ReviewImage::getUrl)
                    .collect(Collectors.toList());
        }
        return null;
    }
}
