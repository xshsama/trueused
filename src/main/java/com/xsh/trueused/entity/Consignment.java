package com.xsh.trueused.entity;

import java.math.BigDecimal;

import com.xsh.trueused.common.BaseEntity;
import com.xsh.trueused.enums.ConsignmentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "consignments")
public class Consignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "expected_price", precision = 10, scale = 2)
    private BigDecimal expectedPrice;

    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "shipping_method")
    private String shippingMethod;

    @Column(name = "tracking_no_inbound")
    private String trackingNoInbound;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsignmentStatus status = ConsignmentStatus.CREATED;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
}
