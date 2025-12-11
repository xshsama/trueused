package com.xsh.trueused.dto;

import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class InspectionFlowDTO {
    private Long inspectionId;
    private Long orderId;
    private String status;
    private String resultSummary;
    private String productTitle;
    private String productImage;
    private String categoryName;
    private String grade;
    private Instant createdAt;
    private Instant updatedAt;
    private List<InspectionItemResultDTO> items;
}
