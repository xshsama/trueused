package com.xsh.trueused.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xsh.trueused.entity.RefundRequest;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    Optional<RefundRequest> findByOrderId(Long orderId);
}
