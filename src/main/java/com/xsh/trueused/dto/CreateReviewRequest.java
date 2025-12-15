package com.xsh.trueused.dto;

import java.util.List;

import lombok.Data;

@Data
public class CreateReviewRequest {
    private Long orderId;
    private Integer rating;
    private String content;
    private Boolean isAnonymous;
    private List<String> images;
}
