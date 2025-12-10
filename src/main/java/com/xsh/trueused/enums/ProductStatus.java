package com.xsh.trueused.enums;

/**
 * 商品状态枚举 (Consignment Lifecycle)
 */
public enum ProductStatus {
    // 待入仓阶段
    CREATED, // 卖家提交申请 (待发货)
    SHIPPED, // 卖家已发货 (运输中)
    RECEIVED, // 仓库已签收

    // 验货阶段
    INSPECTING, // 验货中
    WAREHOUSED, // 已入库 (验货通过)
    REJECTED, // 验货驳回
    RETURNED, // 已退回 (平台退回给卖家)

    // 销售阶段
    ON_SALE, // 出售中 (已上架)
    LOCKED, // 已锁定 (买家下单未付款)
    SOLD_OUT, // 已售出 (买家已付款)

    // 其他
    CANCELLED // 已取消 (卖家主动取消)
}
