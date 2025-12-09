package com.xsh.trueused.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.InspectionResult;

@Repository
public interface InspectionResultRepository extends JpaRepository<InspectionResult, Long> {
    List<InspectionResult> findByInspectionId(Long inspectionId);
}
