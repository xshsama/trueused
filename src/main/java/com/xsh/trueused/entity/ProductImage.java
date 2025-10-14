package com.xsh.trueused.entity;

import com.xsh.trueused.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_images", indexes = {
        @Index(name = "idx_images_product", columnList = "product_id")
})
public class ProductImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 255)
    private String url;

    @Column(name = "sort", nullable = false)
    private Integer sort = 0;

    @Column(name = "is_cover", nullable = false)
    private Boolean isCover = Boolean.FALSE;
}
