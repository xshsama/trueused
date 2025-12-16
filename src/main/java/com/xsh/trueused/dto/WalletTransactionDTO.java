package com.xsh.trueused.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.xsh.trueused.enums.WalletTransactionStatus;
import com.xsh.trueused.enums.WalletTransactionType;

import lombok.Data;

@Data
public class WalletTransactionDTO {
    private Long id;
    private BigDecimal amount;
    private WalletTransactionType type;
    private Long orderId;
    private WalletTransactionStatus status;
    private String remark;
    private LocalDateTime createdAt;
}
