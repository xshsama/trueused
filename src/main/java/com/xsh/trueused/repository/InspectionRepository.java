package com.xsh.trueused.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.Inspection;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    Optional<Inspection> findByOrderId(Long orderId);

    Optional<Inspection> findByConsignmentId(Long consignmentId);
}
