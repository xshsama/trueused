package com.xsh.trueused.entity;

import com.xsh.trueused.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "inspections")
public class Inspection extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, FAILED

    @Column(name = "result_summary")
    private String resultSummary;
}
