package com.xsh.trueused.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.xsh.trueused.common.BaseEntity;
import com.xsh.trueused.enums.ProductCondition;
import com.xsh.trueused.enums.ProductStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products", indexes = {
                @Index(name = "idx_products_seller", columnList = "seller_id"),
                @Index(name = "idx_products_category", columnList = "category_id"),
                @Index(name = "idx_products_status", columnList = "status"),
                @Index(name = "idx_products_price", columnList = "price"),
                @Index(name = "idx_products_created_at", columnList = "created_at"),
                @Index(name = "idx_products_title", columnList = "title")
})
public class Product extends BaseEntity {

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "seller_id", nullable = false)
        private User seller;

        @Column(nullable = false, length = 120)
        private String title;

        @Column(nullable = false, columnDefinition = "TEXT")
        private String description;

        @Column(nullable = false, precision = 12, scale = 2)
        private BigDecimal price;

        @Column(length = 3, nullable = false)
        private String currency = "CNY";

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private ProductStatus status = ProductStatus.DRAFT;

        @Enumerated(EnumType.STRING)
        @Column(name = "item_condition", length = 20)
        private ProductCondition condition;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "category_id")
        private Category category;

        @Column(name = "location_text", length = 100)
        private String locationText;

        @Column
        private Double lat;

        @Column
        private Double lng;

        @Column(name = "views_count", nullable = false)
        private Long viewsCount = 0L;

        @Column(name = "favorites_count", nullable = false)
        private Long favoritesCount = 0L;

        @Column(name = "is_deleted", nullable = false)
        private Boolean isDeleted = Boolean.FALSE;

        @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<ProductImage> images = new ArrayList<>();
}
