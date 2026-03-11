package com.xsh.trueused.refund.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.xsh.trueused.enums.RefundStatus;
import com.xsh.trueused.enums.RefundType;

import lombok.Data;

@Data
public class RefundRequestDTO {
    private Long id;
    private Long orderId;
    private RefundType refundType;
    private RefundStatus status;
    private BigDecimal refundAmount;
    private String reason;
    private Instant createdAt;
    private Instant updatedAt;
}
