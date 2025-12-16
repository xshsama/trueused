package com.xsh.trueused.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.xsh.trueused.entity.Product;
import com.xsh.trueused.enums.ProductStatus;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findByStatus(ProductStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(p.viewsCount) FROM Product p WHERE p.seller.id = :sellerId")
    Long sumViewsBySellerId(Long sellerId);
}
