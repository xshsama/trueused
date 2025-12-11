package com.xsh.trueused.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.xsh.trueused.enums.ConsignmentStatus;

import lombok.Data;

@Data
public class ConsignmentResponse {
    private Long id;
    private PublicUserDTO seller;
    private String title;
    private String description;
    private BigDecimal expectedPrice;
    private String shippingMethod;
    private String trackingNoInbound;
    private ConsignmentStatus status;
    private ProductDTO product;
    private Instant createdAt;
    private Instant updatedAt;
}
