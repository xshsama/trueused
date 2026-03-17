package com.xsh.trueused.refund.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xsh.trueused.entity.RefundRequest;
import com.xsh.trueused.enums.RefundStatus;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    Optional<RefundRequest> findByOrderId(Long orderId);

    List<RefundRequest> findByStatusAndUpdatedAtBefore(RefundStatus status, Instant updatedAt);
}
