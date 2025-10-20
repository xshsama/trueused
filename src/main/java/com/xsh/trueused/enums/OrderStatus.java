package com.xsh.trueused.enums;

/**
 * 订单状态枚举
 */
public enum OrderStatus {
    PENDING, // 待处理/待付款
    PAID, // 已付款/待发货
    SHIPPED, // 已发货
    DELIVERED, // 已送达
    COMPLETED, // 已完成
    CANCELLED, // 已取消
    REFUNDED // 已退款
}