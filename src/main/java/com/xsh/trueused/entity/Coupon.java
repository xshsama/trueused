package com.xsh.trueused.entity;

import java.math.BigDecimal;

import com.xsh.trueused.common.BaseEntity;
import com.xsh.trueused.enums.CouponType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "coupons")
public class Coupon extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType type = CouponType.DISCOUNT;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "min_spend")
    private BigDecimal minSpend = BigDecimal.ZERO;

    @Column(name = "valid_days")
    private Integer validDays = 30;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
