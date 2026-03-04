package com.xsh.trueused.inspection.dto;

import java.time.Instant;

import lombok.Data;

@Data
public class InspectionItemResultDTO {
    private Long id;
    private Long itemId;
    private String itemName;
    private String itemDescription;
    private Integer sequenceOrder;
    private String status;
    private String notes;
    private String imageUrl;
    private Instant updatedAt;
}
