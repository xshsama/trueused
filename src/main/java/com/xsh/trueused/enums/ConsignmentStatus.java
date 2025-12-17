package com.xsh.trueused.enums;

public enum ConsignmentStatus {
    // --- 卖家侧流程 ---
    CREATED, // 已创建 (等待卖家发货)
    SHIPPED, // 卖家已发货 (等待仓库签收)

    // --- 平台侧流程 ---
    RECEIVED, // 仓库已签收 (等待质检)
    INSPECTING, // 正在质检中

    // --- 质检结果分支 ---
    PASSED, // 质检通过 (--> 触发 Product 变更为 ON_SALE)
    REJECTED, // 质检驳回 (--> 触发 Product 保持 PENDING 或 OFF_SHELF)

    // --- 异常/售后流程 ---
    RETURNING, // 退回中 (验货不通过，平台寄回给卖家)
    RETURNED, // 已退回 (卖家签收退件)

    CANCELLED // 卖家取消寄卖 (仅在发货前可取消)
}
