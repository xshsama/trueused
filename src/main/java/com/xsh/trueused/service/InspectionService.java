package com.xsh.trueused.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.xsh.trueused.dto.InspectionFlowDTO;
import com.xsh.trueused.dto.InspectionItemResultDTO;
import com.xsh.trueused.entity.Category;
import com.xsh.trueused.entity.Consignment;
import com.xsh.trueused.entity.Inspection;
import com.xsh.trueused.entity.InspectionItem;
import com.xsh.trueused.entity.InspectionResult;
import com.xsh.trueused.entity.Order;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.enums.ConsignmentStatus;
import com.xsh.trueused.repository.ConsignmentRepository;
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
    private final ConsignmentRepository consignmentRepository;
    private final ProductService productService;
    private final ApplicationContext applicationContext;

    private final Random random = new Random();

    @Transactional
    public InspectionFlowDTO triggerInspection(Long orderId) {
        log.info("Triggering inspection for order: {}", orderId);

        // Check if inspection already exists
        Optional<Inspection> existingInspection = inspectionRepository.findByOrderId(orderId);
        if (existingInspection.isPresent()) {
            Inspection inspection = existingInspection.get();
            // If stuck in PENDING for some reason, restart simulation
            if ("PENDING".equals(inspection.getStatus())) {
                log.info("Restarting stuck inspection simulation for order: {}", orderId);
                scheduleSimulation(inspection.getId());
            }
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

        // Start simulation asynchronously AFTER commit
        scheduleSimulation(inspection.getId());

        return getInspectionFlow(orderId);
    }

    @Transactional
    public void triggerInspectionForConsignment(Long consignmentId) {
        log.info("Triggering inspection for consignment: {}", consignmentId);

        Consignment consignment = consignmentRepository.findById(consignmentId)
                .orElseThrow(() -> new RuntimeException("Consignment not found"));

        consignment.setStatus(ConsignmentStatus.INSPECTING);
        consignmentRepository.save(consignment);

        Inspection inspection = new Inspection();
        inspection.setConsignment(consignment);
        inspection.setStatus("PENDING");
        inspection = inspectionRepository.save(inspection);

        // Determine Template Type based on Category
        String templateType = determineTemplateType(consignment.getCategory());

        // Create Inspection Results (initially PENDING)
        List<InspectionItem> items = inspectionItemRepository.findByTemplateTypeOrderBySequenceOrderAsc(templateType);
        // Fallback to BASIC if no items found
        if (items.isEmpty()) {
            items = inspectionItemRepository.findByTemplateTypeOrderBySequenceOrderAsc("BASIC");
        }

        List<InspectionResult> results = new ArrayList<>();

        for (InspectionItem item : items) {
            InspectionResult result = new InspectionResult();
            result.setInspection(inspection);
            result.setItem(item);
            result.setStatus("PENDING");
            results.add(result);
        }
        inspectionResultRepository.saveAll(results);

        // Start simulation AFTER commit
        scheduleSimulation(inspection.getId());
    }

    private void scheduleSimulation(Long inspectionId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // Use self-reference to invoke async method
                    InspectionService self = applicationContext.getBean(InspectionService.class);
                    self.simulateInspectionProcess(inspectionId);
                }
            });
        } else {
            // No transaction, run immediately
            InspectionService self = applicationContext.getBean(InspectionService.class);
            self.simulateInspectionProcess(inspectionId);
        }
    }

    private String determineTemplateType(Category category) {
        if (category == null)
            return "BASIC";

        // Traverse up to find root category or match known slugs
        Category current = category;
        while (current != null) {
            // Check by ID (Root IDs: 1=Digital, 2=Clothing, 3=Home, 4=Books, 5=Sports)
            if (current.getId() == 1L)
                return "DIGITAL";
            if (current.getId() == 2L)
                return "CLOTHING";
            if (current.getId() == 3L)
                return "LIFESTYLE";
            if (current.getId() == 4L)
                return "BOOKS";
            if (current.getId() == 5L)
                return "BASIC"; // Sports maps to Basic

            current = current.getParent();
        }
        return "BASIC";
    }

    @Transactional(readOnly = true)
    public InspectionFlowDTO getInspectionFlow(Long orderId) {
        Inspection inspection = inspectionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Inspection not found for order " + orderId));

        return toInspectionFlowDTO(inspection);
    }

    @Transactional(readOnly = true)
    public InspectionFlowDTO getInspectionFlowByConsignment(Long consignmentId) {
        Inspection inspection = inspectionRepository.findByConsignmentId(consignmentId)
                .orElseThrow(() -> new RuntimeException("Inspection not found for consignment " + consignmentId));

        return toInspectionFlowDTO(inspection);
    }

    @Transactional(readOnly = true)
    public List<InspectionFlowDTO> getMyInspections(Long userId) {
        // Find inspections where the user is the seller (via Consignment or Order)
        // This is a bit complex, so we might need a custom query in repository
        // For now, let's fetch all inspections and filter (not efficient but works for
        // MVP)
        List<Inspection> allInspections = inspectionRepository.findAll();
        return allInspections.stream()
                .filter(i -> isUserRelatedToInspection(i, userId))
                .map(this::toInspectionFlowDTO)
                .collect(Collectors.toList());
    }

    private boolean isUserRelatedToInspection(Inspection i, Long userId) {
        if (i.getConsignment() != null && i.getConsignment().getSeller().getId().equals(userId)) {
            return true;
        }
        if (i.getOrder() != null && (i.getOrder().getBuyer().getId().equals(userId)
                || i.getOrder().getSeller().getId().equals(userId))) {
            return true;
        }
        return false;
    }

    private InspectionFlowDTO toInspectionFlowDTO(Inspection inspection) {
        List<InspectionResult> results = inspectionResultRepository.findByInspectionId(inspection.getId());

        InspectionFlowDTO dto = new InspectionFlowDTO();
        dto.setInspectionId(inspection.getId());
        if (inspection.getOrder() != null) {
            dto.setOrderId(inspection.getOrder().getId());
        }
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

        // Populate product info from Consignment or Order
        if (inspection.getConsignment() != null) {
            Consignment c = inspection.getConsignment();
            dto.setProductTitle(c.getTitle());
            dto.setCategoryName(c.getCategory() != null ? c.getCategory().getName() : "Unknown");
            // Assuming Consignment has product linked or we use placeholder
            if (c.getProduct() != null && !c.getProduct().getImages().isEmpty()) {
                dto.setProductImage(c.getProduct().getImages().get(0).getUrl());
            }
            // Determine grade based on result summary or status
            dto.setGrade(determineGrade(inspection));
        } else if (inspection.getOrder() != null) {
            Order o = inspection.getOrder();
            if (o.getProduct() != null) {
                Product p = o.getProduct();
                dto.setProductTitle(p.getTitle());
                dto.setCategoryName(p.getCategory() != null ? p.getCategory().getName() : "Unknown");
                if (!p.getImages().isEmpty()) {
                    dto.setProductImage(p.getImages().get(0).getUrl());
                }
            }
            dto.setGrade(determineGrade(inspection));
        }

        return dto;
    }

    private String determineGrade(Inspection inspection) {
        if ("FAILED".equals(inspection.getStatus())) {
            return "X";
        }
        // Simple logic: if summary contains "good", return S, else A
        String summary = inspection.getResultSummary();
        if (summary != null && summary.contains("good")) {
            return "S";
        }
        return "A";
    }

    @Async
    public void simulateInspectionProcess(Long inspectionId) {
        // Use self-reference to invoke transactional methods via proxy
        InspectionService self = applicationContext.getBean(InspectionService.class);

        try {
            log.info("Starting inspection simulation for inspection: {}", inspectionId);

            // Update status to IN_PROGRESS
            self.updateInspectionStatus(inspectionId, "IN_PROGRESS");
            Thread.sleep(2000); // Wait a bit

            List<InspectionResult> results = inspectionResultRepository.findByInspectionId(inspectionId);
            // Sort by sequence order
            results.sort((r1, r2) -> r1.getItem().getSequenceOrder().compareTo(r2.getItem().getSequenceOrder()));

            for (InspectionResult result : results) {
                // Random delay between 2s and 5s
                long delay = 2000 + random.nextInt(3000);
                Thread.sleep(delay);

                result.setStatus("PASSED");
                result.setNotes(getRandomNote(result.getItem().getName()));
                result.setUpdatedAt(Instant.now());
                inspectionResultRepository.save(result);
                log.info("Inspection item {} passed", result.getItem().getName());
            }

            // Complete inspection
            self.updateInspectionStatus(inspectionId, "COMPLETED");
            self.updateInspectionSummary(inspectionId, "All checks passed. Device is in good condition.");
            log.info("Inspection simulation completed for inspection: {}", inspectionId);

            // Handle Consignment Success
            Inspection inspection = inspectionRepository.findById(inspectionId).orElseThrow();
            if (inspection.getConsignment() != null) {
                self.handleConsignmentInspectionSuccess(inspection.getConsignment().getId());
            }

        } catch (Exception e) {
            log.error("Error during inspection simulation", e);
            self.updateInspectionStatus(inspectionId, "FAILED");
        }
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

    @Transactional
    public void handleConsignmentInspectionSuccess(Long consignmentId) {
        Consignment consignment = consignmentRepository.findById(consignmentId).orElseThrow();
        consignment.setStatus(ConsignmentStatus.PASSED);
        consignmentRepository.save(consignment);
        productService.createFromConsignment(consignment);
    }

    private String getRandomNote(String itemName) {
        String[] commonNotes = { "检查通过，状况良好", "功能正常", "无明显瑕疵", "符合标准" };
        String[] screenNotes = { "屏幕显示清晰", "无坏点", "触控灵敏", "玻璃完好" };
        String[] batteryNotes = { "电池健康度 95%", "电池健康度 88%", "电池健康度 92%", "充放电正常" };
        String[] appearanceNotes = { "外观95新", "轻微使用痕迹", "外观完好", "无磕碰" };

        if (itemName.contains("屏幕"))
            return screenNotes[random.nextInt(screenNotes.length)];
        if (itemName.contains("电池"))
            return batteryNotes[random.nextInt(batteryNotes.length)];
        if (itemName.contains("外观") || itemName.contains("破损") || itemName.contains("划痕"))
            return appearanceNotes[random.nextInt(appearanceNotes.length)];

        return commonNotes[random.nextInt(commonNotes.length)];
    }
}
