package com.xsh.trueused.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class WalletDTO {
    private Long id;
    private BigDecimal balance;
    private BigDecimal frozenAmount;
    private boolean hasPayPassword;
}
