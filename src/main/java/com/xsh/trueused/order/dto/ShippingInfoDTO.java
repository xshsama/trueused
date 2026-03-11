package com.xsh.trueused.order.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 物流信息 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingInfoDTO {

    /**
     * 发货类型: SELLER_OUTBOUND / PLATFORM_OUTBOUND
     */
    private String shipmentType;

    /**
     * 快递单号
     */
    private String trackingNumber;

    /**
     * 快递公司
     */
    private String expressCompany;

    /**
     * 快递公司编码
     */
    private String expressCode;

    /**
     * 物流状态: PENDING-待揽收, PICKED-已揽收, IN_TRANSIT-运输中, DELIVERING-派送中, DELIVERED-已签收
     */
    private String shippingStatus;

    /**
     * 发货时间
     */
    private Instant shippedAt;

    /**
     * 预计送达时间
     */
    private Instant estimatedDeliveryTime;

    /**
     * 发件城市
     */
    private String senderCity;

    /**
     * 发件区县
     */
    private String senderDistrict;

    /**
     * 收件城市
     */
    private String receiverCity;

    /**
     * 收件区县
     */
    private String receiverDistrict;

    /**
     * 物流轨迹
     */
    private List<TrackingEvent> trackingEvents;

    /**
     * 物流轨迹事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingEvent {
        /**
         * 事件时间
         */
        private Instant time;

        /**
         * 事件描述
         */
        private String description;

        /**
         * 事件地点
         */
        private String location;

        /**
         * 事件状态
         */
        private String status;
    }
}
