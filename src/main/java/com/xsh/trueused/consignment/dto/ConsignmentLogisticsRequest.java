package com.xsh.trueused.consignment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConsignmentLogisticsRequest {
    @NotBlank(message = "Tracking number is required")
    private String trackingNo;
}
