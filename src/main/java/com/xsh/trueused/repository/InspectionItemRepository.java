package com.xsh.trueused.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.InspectionItem;

@Repository
public interface InspectionItemRepository extends JpaRepository<InspectionItem, Long> {
    List<InspectionItem> findAllByOrderBySequenceOrderAsc();
}
