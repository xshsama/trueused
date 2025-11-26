package com.xsh.trueused.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.xsh.trueused.common.BaseEntity;
import com.xsh.trueused.enums.OrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // 物流信息字段
    @Column(name = "tracking_number", length = 50)
    private String trackingNumber;

    @Column(name = "express_company", length = 50)
    private String expressCompany;

    @Column(name = "express_code", length = 20)
    private String expressCode;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "estimated_delivery_time")
    private Instant estimatedDeliveryTime;

    @Column(name = "delivered_at")
    private Instant deliveredAt;
}