package com.xsh.trueused.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class WalletTopUpRequest {
    private BigDecimal amount;
}
