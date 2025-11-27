package com.xsh.trueused.dto;

import lombok.Data;

@Data
public class CreateReviewRequest {
    private Long orderId;
    private Integer rating;
    private String content;
    private Boolean isAnonymous;
}
