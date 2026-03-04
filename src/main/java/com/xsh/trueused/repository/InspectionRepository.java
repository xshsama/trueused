package com.xsh.trueused.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.Inspection;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    Optional<Inspection> findByOrderId(Long orderId);

    Optional<Inspection> findByConsignmentId(Long consignmentId);

    @Query("""
            select coalesce(cc.name, pc.name)
            from Inspection i
            left join i.consignment c
            left join c.category cc
            left join i.order o
            left join o.product p
            left join p.category pc
            where i.id = :inspectionId
            """)
    Optional<String> findCategoryNameByInspectionId(@Param("inspectionId") Long inspectionId);
}
