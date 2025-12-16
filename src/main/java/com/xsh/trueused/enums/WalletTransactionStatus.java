package com.xsh.trueused.enums;

public enum WalletTransactionStatus {
    PENDING("处理中"),
    SUCCESS("成功"),
    FAILED("失败");

    private final String description;

    WalletTransactionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
