package com.xsh.trueused.entity;

import java.util.ArrayList;
import java.util.List;

import com.xsh.trueused.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_categories_parent", columnList = "parent_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_categories_name_parent", columnNames = { "name", "parent_id" })
})
public class Category extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> children = new ArrayList<>();

    @Column(length = 50, unique = true)
    private String slug;

    @Column(length = 200)
    private String path;

    @Column(length = 20)
    private String status = "ACTIVE"; // 简化: ACTIVE / INACTIVE
}
