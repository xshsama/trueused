package com.xsh.trueused.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Data;

@Data
public class CouponDTO {
    private Long id;
    private String code;
    private String title;
    private String description;
    private BigDecimal discountAmount;
    private BigDecimal minSpend;
    private Integer validDays;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
