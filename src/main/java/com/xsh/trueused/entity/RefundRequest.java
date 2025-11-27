package com.xsh.trueused.entity;

import java.math.BigDecimal;

import com.xsh.trueused.common.BaseEntity;
import com.xsh.trueused.enums.RefundStatus;
import com.xsh.trueused.enums.RefundType;

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
@Table(name = "refund_requests")
public class RefundRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_type", nullable = false)
    private RefundType refundType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    @Column(name = "refund_amount", nullable = false)
    private BigDecimal refundAmount;
}
