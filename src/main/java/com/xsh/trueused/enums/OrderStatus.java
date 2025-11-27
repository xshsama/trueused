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
    REFUND_PENDING, // 退款申请中
    REFUND_APPROVED, // 退款已同意
    RETURN_PENDING, // 等待退货
    REFUNDED // 已退款
}