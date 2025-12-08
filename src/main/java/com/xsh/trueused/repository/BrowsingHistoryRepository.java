package com.xsh.trueused.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.xsh.trueused.entity.BrowsingHistory;

public interface BrowsingHistoryRepository extends JpaRepository<BrowsingHistory, Long> {
    Page<BrowsingHistory> findByUserIdOrderByViewedAtDesc(Long userId, Pageable pageable);

    // Optional: Find existing record to update timestamp instead of creating new
    // one
    BrowsingHistory findByUserIdAndProductId(Long userId, Long productId);
}
