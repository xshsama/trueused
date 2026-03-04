package com.xsh.trueused.order.enums;

/**
 * 订单状态枚举 (Order Lifecycle)
 */
public enum OrderStatus {
    PENDING_PAYMENT, // 待付款 (买家下单)
    PAID, // 已付款 (支付成功)
    PENDING_SHIPMENT, // 待发货 (系统推送到仓库)
    SHIPPED, // 已发货 (仓库打单发货)
    COMPLETED, // 已完成 (买家签收/确认)

    REFUNDING, // 售后中 (买家申请退货)
    REFUNDED, // 已退款 (平台确认退货入库)

    CANCELLED // 已取消 (超时/手动取消)
}