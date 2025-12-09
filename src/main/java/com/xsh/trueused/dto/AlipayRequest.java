package com.xsh.trueused.dto;

import lombok.Data;

@Data
public class AlipayRequest {
    private String outTradeNo;
    private String totalAmount;
    private String subject;
    private String body;
}
