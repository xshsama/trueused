package com.xsh.trueused.enums;

public enum WalletTransactionType {
    TOP_UP("充值"),
    PAYMENT("支付"),
    INCOME("收入"),
    REFUND("退款"),
    WITHDRAWAL("提现");

    private final String description;

    WalletTransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
