package com.xsh.trueused.dto;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private Long productId;
    private Long addressId;
}