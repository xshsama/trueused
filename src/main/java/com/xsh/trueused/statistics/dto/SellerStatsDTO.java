package com.xsh.trueused.statistics.dto;

import java.math.BigDecimal;

public record SellerStatsDTO(
        long onShelfProducts,
        long pendingOrders,
        long violationProducts,
        BigDecimal totalIncome,
        long unreadMessages,
        long totalViews) {
}
