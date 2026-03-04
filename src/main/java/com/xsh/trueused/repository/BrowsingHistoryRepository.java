package com.xsh.trueused.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.xsh.trueused.entity.BrowsingHistory;
import com.xsh.trueused.enums.ProductStatus;

public interface BrowsingHistoryRepository extends JpaRepository<BrowsingHistory, Long> {
    Page<BrowsingHistory> findByUserIdOrderByViewedAtDesc(Long userId, Pageable pageable);

    // Optional: Find existing record to update timestamp instead of creating new
    // one
    BrowsingHistory findByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT COUNT(DISTINCT bh.user.id) FROM BrowsingHistory bh WHERE bh.product.seller.id = :sellerId AND bh.viewedAt BETWEEN :start AND :end")
    Long countDistinctUserByProductSellerAndViewedAtBetween(Long sellerId, Instant start, Instant end);

    @Query("""
            SELECT COUNT(DISTINCT bh.user.id)
            FROM BrowsingHistory bh
            WHERE bh.product.seller.id = :sellerId
              AND bh.viewedAt BETWEEN :start AND :end
              AND bh.product.status IN :productStatuses
            """)
    Long countDistinctUserByProductSellerAndViewedAtBetweenAndProductStatusIn(
            Long sellerId,
            Instant start,
            Instant end,
            List<ProductStatus> productStatuses);

    @Query("SELECT bh FROM BrowsingHistory bh WHERE bh.product.seller.id = :sellerId AND bh.viewedAt BETWEEN :start AND :end")
    List<BrowsingHistory> findByProductSellerIdAndViewedAtBetween(Long sellerId, Instant start, Instant end);

    @Query("""
            SELECT bh
            FROM BrowsingHistory bh
            WHERE bh.product.seller.id = :sellerId
              AND bh.viewedAt BETWEEN :start AND :end
              AND bh.product.status IN :productStatuses
            """)
    List<BrowsingHistory> findByProductSellerIdAndViewedAtBetweenAndProductStatusIn(
            Long sellerId,
            Instant start,
            Instant end,
            List<ProductStatus> productStatuses);
}
