package com.xsh.trueused.enums;

public enum ConsignmentStatus {
    CREATED, // 已创建，待发货
    SHIPPED, // 已发货
    RECEIVED, // 已签收，待验货
    INSPECTING, // 验货中
    PASSED, // 验货通过
    REJECTED, // 验货驳回
    CANCELLED // 已取消
}
