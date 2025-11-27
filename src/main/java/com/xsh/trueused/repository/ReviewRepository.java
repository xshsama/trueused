package com.xsh.trueused.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.xsh.trueused.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductId(Long productId, Pageable pageable);

    List<Review> findByBuyerId(Long buyerId);

    Optional<Review> findByOrderId(Long orderId);
}
