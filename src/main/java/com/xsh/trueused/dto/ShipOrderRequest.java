package com.xsh.trueused.dto;

import lombok.Data;

/**
 * 发货请求 DTO
 */
@Data
public class ShipOrderRequest {

    /**
     * 快递公司名称
     */
    private String expressCompany;

    /**
     * 快递单号（可选，如果不填则自动生成）
     */
    private String trackingNumber;

    /**
     * 发货城市
     */
    private String senderCity;

    /**
     * 发货区/县
     */
    private String senderDistrict;

    /**
     * 发货详细地址
     */
    private String senderAddress;
}
