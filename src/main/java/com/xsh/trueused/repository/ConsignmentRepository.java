package com.xsh.trueused.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.Consignment;
import com.xsh.trueused.enums.ConsignmentStatus;

@Repository
public interface ConsignmentRepository extends JpaRepository<Consignment, Long> {
    List<Consignment> findBySellerId(Long sellerId);

    List<Consignment> findByStatus(ConsignmentStatus status);
}
