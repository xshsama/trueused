package com.xsh.trueused.order.dto;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private Long productId;
    private Long addressId;
    private Long userCouponId;
}