package com.xsh.trueused.entity;

import com.xsh.trueused.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "inspection_results")
public class InspectionResult extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private InspectionItem item;

    @Column(nullable = false)
    private String status; // PENDING, PASSED, FAILED, SKIPPED

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "image_url")
    private String imageUrl;
}
