package com.xsh.trueused.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.xsh.trueused.entity.Product;
import com.xsh.trueused.enums.ProductStatus;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    long countByStatus(ProductStatus status);

    List<Product> findByStatus(ProductStatus status);

    @Query("SELECT SUM(p.viewsCount) FROM Product p WHERE p.seller.id = :sellerId")
    Long sumViewsBySellerId(Long sellerId);

    @Query("SELECT SUM(p.viewsCount) FROM Product p WHERE p.seller.id = :sellerId AND p.status IN :statuses")
    Long sumViewsBySellerIdAndStatusIn(Long sellerId, List<ProductStatus> statuses);

    List<Product> findTop5BySellerIdOrderByViewsCountDesc(Long sellerId);

    List<Product> findTop5BySellerIdAndStatusInOrderByViewsCountDesc(Long sellerId, List<ProductStatus> statuses);

    @Modifying
    @Query("UPDATE Product p SET p.viewsCount = :views WHERE p.id = :id")
    void updateViewsCount(Long id, Long views);
}
