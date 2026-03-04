package com.xsh.trueused.review.dto;

import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class ReviewDTO {
    private Long id;
    private Long orderId;
    private Long productId;
    private String productTitle;
    private String productImage;
    private java.math.BigDecimal price;
    private Long buyerId;
    private String buyerName;
    private String buyerAvatar;
    private Integer rating;
    private String content;
    private Boolean isAnonymous;
    private String sellerReply;
    private Instant sellerReplyAt;
    private Instant createdAt;
    private List<String> images;
}
