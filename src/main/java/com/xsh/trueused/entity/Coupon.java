package com.xsh.trueused.entity;

import java.math.BigDecimal;

import com.xsh.trueused.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
