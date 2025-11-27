package com.xsh.trueused.dto;

import java.math.BigDecimal;

import com.xsh.trueused.enums.RefundType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefundRequestCreateDTO {
    @NotBlank(message = "退款原因不能为空")
    private String reason;

    @NotNull(message = "退款类型不能为空")
    private RefundType refundType;

    @NotNull(message = "退款金额不能为空")
    private BigDecimal refundAmount;
}
