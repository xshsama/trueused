package com.xsh.trueused.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.dto.InspectionFlowDTO;
import com.xsh.trueused.dto.InspectionItemResultDTO;
import com.xsh.trueused.entity.Inspection;
import com.xsh.trueused.entity.InspectionItem;
import com.xsh.trueused.entity.InspectionResult;
import com.xsh.trueused.entity.Order;
import com.xsh.trueused.repository.InspectionItemRepository;
import com.xsh.trueused.repository.InspectionRepository;
import com.xsh.trueused.repository.InspectionResultRepository;
import com.xsh.trueused.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final InspectionItemRepository inspectionItemRepository;
    private final InspectionResultRepository inspectionResultRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public InspectionFlowDTO triggerInspection(Long orderId) {
        log.info("Triggering inspection for order: {}", orderId);

        // Check if inspection already exists
        Optional<Inspection> existingInspection = inspectionRepository.findByOrderId(orderId);
        if (existingInspection.isPresent()) {
            return getInspectionFlow(orderId);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Create Inspection
        Inspection inspection = new Inspection();
        inspection.setOrder(order);
        inspection.setStatus("PENDING");
        inspection = inspectionRepository.save(inspection);

        // Create Inspection Results (initially PENDING)
        List<InspectionItem> items = inspectionItemRepository.findAllByOrderBySequenceOrderAsc();
        List<InspectionResult> results = new ArrayList<>();

        for (InspectionItem item : items) {
            InspectionResult result = new InspectionResult();
            result.setInspection(inspection);
            result.setItem(item);
            result.setStatus("PENDING");
            results.add(result);
        }
        inspectionResultRepository.saveAll(results);

        // Start simulation asynchronously
        simulateInspectionProcess(inspection.getId());

        return getInspectionFlow(orderId);
    }

    public InspectionFlowDTO getInspectionFlow(Long orderId) {
        Inspection inspection = inspectionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Inspection not found for order " + orderId));

        List<InspectionResult> results = inspectionResultRepository.findByInspectionId(inspection.getId());

        InspectionFlowDTO dto = new InspectionFlowDTO();
        dto.setInspectionId(inspection.getId());
        dto.setOrderId(orderId);
        dto.setStatus(inspection.getStatus());
        dto.setResultSummary(inspection.getResultSummary());
        dto.setCreatedAt(inspection.getCreatedAt());
        dto.setUpdatedAt(inspection.getUpdatedAt());

        List<InspectionItemResultDTO> itemDTOs = results.stream().map(result -> {
            InspectionItemResultDTO itemDTO = new InspectionItemResultDTO();
            itemDTO.setId(result.getId());
            itemDTO.setItemId(result.getItem().getId());
            itemDTO.setItemName(result.getItem().getName());
            itemDTO.setItemDescription(result.getItem().getDescription());
            itemDTO.setSequenceOrder(result.getItem().getSequenceOrder());
            itemDTO.setStatus(result.getStatus());
            itemDTO.setNotes(result.getNotes());
            itemDTO.setImageUrl(result.getImageUrl());
            itemDTO.setUpdatedAt(result.getUpdatedAt());
            return itemDTO;
        }).collect(Collectors.toList());

        dto.setItems(itemDTOs);
        return dto;
    }

    @Async
    public void simulateInspectionProcess(Long inspectionId) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting inspection simulation for inspection: {}", inspectionId);

                // Update status to IN_PROGRESS
                updateInspectionStatus(inspectionId, "IN_PROGRESS");
                Thread.sleep(2000); // Wait a bit

                List<InspectionResult> results = inspectionResultRepository.findByInspectionId(inspectionId);
                // Sort by sequence order
                results.sort((r1, r2) -> r1.getItem().getSequenceOrder().compareTo(r2.getItem().getSequenceOrder()));

                for (InspectionResult result : results) {
                    Thread.sleep(3000); // Simulate time taken for each step
                    result.setStatus("PASSED");
                    result.setNotes("Check passed successfully.");
                    result.setUpdatedAt(Instant.now());
                    inspectionResultRepository.save(result);
                    log.info("Inspection item {} passed", result.getItem().getName());
                }

                // Complete inspection
                updateInspectionStatus(inspectionId, "COMPLETED");
                updateInspectionSummary(inspectionId, "All checks passed. Device is in good condition.");
                log.info("Inspection simulation completed for inspection: {}", inspectionId);

            } catch (Exception e) {
                log.error("Error during inspection simulation", e);
                updateInspectionStatus(inspectionId, "FAILED");
            }
        });
    }

    @Transactional
    public void updateInspectionStatus(Long inspectionId, String status) {
        Inspection inspection = inspectionRepository.findById(inspectionId).orElseThrow();
        inspection.setStatus(status);
        inspectionRepository.save(inspection);
    }

    @Transactional
    public void updateInspectionSummary(Long inspectionId, String summary) {
        Inspection inspection = inspectionRepository.findById(inspectionId).orElseThrow();
        inspection.setResultSummary(summary);
        inspectionRepository.save(inspection);
    }
}
