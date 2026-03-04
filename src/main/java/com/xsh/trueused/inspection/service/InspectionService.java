package com.xsh.trueused.inspection.service;

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

import com.xsh.trueused.inspection.dto.InspectionFlowDTO;
import com.xsh.trueused.inspection.dto.InspectionItemResultDTO;
import com.xsh.trueused.entity.Category;
import com.xsh.trueused.entity.Consignment;
import com.xsh.trueused.entity.Inspection;
import com.xsh.trueused.entity.InspectionItem;
import com.xsh.trueused.entity.InspectionResult;
import com.xsh.trueused.entity.Order;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.enums.ConsignmentStatus;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.consignment.repository.ConsignmentRepository;
import com.xsh.trueused.inspection.repository.InspectionItemRepository;
import com.xsh.trueused.inspection.repository.InspectionRepository;
import com.xsh.trueused.inspection.repository.InspectionResultRepository;
import com.xsh.trueused.order.repository.OrderRepository;
import com.xsh.trueused.notification.service.NotificationService;
import com.xsh.trueused.product.service.ProductService;
import com.xsh.trueused.util.CloudinaryUrlHelper;

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
    private final NotificationService notificationService;
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
        // 商品状态保持 PENDING，无需更新
        // if (consignment.getProduct() != null) {
        // consignment.getProduct().setStatus(ProductStatus.PENDING);
        // productService.updateProductStatus(consignment.getProduct().getId(),
        // ProductStatus.PENDING);
        // }
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
    public InspectionFlowDTO getInspectionById(Long inspectionId) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new RuntimeException("Inspection not found with id " + inspectionId));
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
        dto.setResultSummary(sanitizeSummary(inspection.getResultSummary()));
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
                dto.setProductImage(CloudinaryUrlHelper.getUrl(c.getProduct().getImages().get(0).getImageKey()));
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
                    dto.setProductImage(CloudinaryUrlHelper.getUrl(p.getImages().get(0).getImageKey()));
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

    private String generateSmartSummary(Inspection inspection) {
        String categoryName = inspectionRepository.findCategoryNameByInspectionId(inspection.getId()).orElse("");

        String grade = determineGrade(inspection);
        
        // 1. Identify Type
        String cat = (categoryName != null) ? categoryName.toLowerCase() : "";
        String type = "general";
        
        if (cat.contains("手机") || cat.contains("iphone") || cat.contains("ipad") || cat.contains("mac") || cat.contains("电脑") || cat.contains("数码")) {
            type = "digital";
        } else if (cat.contains("衣") || cat.contains("服装") || cat.contains("鞋") || cat.contains("包") || cat.contains("表") || cat.contains("配饰")) {
            type = "fashion";
        } else if (cat.contains("书") || cat.contains("绘本")) {
            type = "book";
        }

        // 2. Grade Intro
        String gradeText;
        switch (grade) {
            case "S": gradeText = "成色完美，整体不仅几乎全新，且未见明显使用痕迹"; break;
            case "A": gradeText = "成色优秀，机身仅有极细微生活痕迹，整体观感与手感俱佳。"; break;
            case "B": gradeText = "成色良好，存在可见使用痕迹或轻微磕碰，但不影响核心功能体验，性价比较高。"; break;
            case "C": gradeText = "成色一般，有明显磨损或功能性小瑕疵，建议仔细查看实拍图。"; break;
            case "X": gradeText = "未通过质检，存在严重功能故障或不符合平台收录标准。"; break;
            default: gradeText = "商品状态符合发布描述，整体状况稳定。";
        }

        // 3. Category Specific Text
        StringBuilder text = new StringBuilder();
        if ("digital".equals(type)) {
            text.append("经 TrueUsed 数码实验室 32 项深度检测，该设备").append(gradeText).append(" ");
            text.append("屏幕显示通透无老化，触控灵敏，核心主板及各项传感器运行稳定。");
        } else if ("fashion".equals(type)) {
            text.append("经 TrueUsed 资深鉴定师查验，该商品").append(gradeText).append(" ");
            text.append("整体版型保持良好，面料无明显变色，五金光泽度自然，走线细节完好。");
        } else if ("book".equals(type)) {
            text.append("经 TrueUsed 图书鉴别团队审核，该书").append(gradeText).append(" ");
            text.append("书页无明显折痕或笔记，书脊完好，装订紧实，阅读体验极佳。");
        } else {
            text.append("经 TrueUsed 官方多重质检，该商品").append(gradeText).append(" ");
            text.append("各项功能正常，外观与描述相符，符合平台严选标准。");
        }
        
        // 4. Issues Note
        List<InspectionResult> issues = inspectionResultRepository.findByInspectionId(inspection.getId()).stream()
                .filter(r -> !"PASSED".equals(r.getStatus()))
                .collect(Collectors.toList());

        if (!issues.isEmpty()) {
            text.append(" 需要注意的是，质检工程师发现以下细节：");
            List<String> issueTexts = issues.stream()
                .map(i -> i.getItem().getName() + (i.getNotes() != null && !i.getNotes().isEmpty() ? "存在" + i.getNotes() : "异常"))
                .collect(Collectors.toList());
            text.append(String.join("、", issueTexts)).append("。");
        } else {
            text.append(" 本次检测未发现功能性故障或隐形暗病，请放心使用。");
        }

        return text.toString();
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
            
            // Generate Smart Summary (Chinese)
            Inspection updatedInspection = inspectionRepository.findById(inspectionId).orElseThrow();
            String smartSummary = generateSmartSummary(updatedInspection);
            self.updateInspectionSummary(inspectionId, smartSummary);
            
            log.info("Inspection simulation completed for inspection: {}", inspectionId);

            // Notify user
            Inspection inspection = inspectionRepository.findById(inspectionId).orElseThrow();
            Long userId = null;
            if (inspection.getOrder() != null) {
                userId = inspection.getOrder().getBuyer().getId();
            } else if (inspection.getConsignment() != null) {
                userId = inspection.getConsignment().getSeller().getId();
            }

            if (userId != null) {
                try {
                    notificationService.createNotification(
                            userId,
                            "验货完成",
                            "您的商品验货已完成，结果：通过。",
                            "INSPECTION_COMPLETED",
                            inspectionId);
                } catch (Exception ex) {
                    log.error("Failed to send inspection success notification for inspection {}", inspectionId, ex);
                }
            }

            // Post-processing should not rewrite successful inspection into FAILED.
            if (inspection.getConsignment() != null) {
                try {
                    self.handleConsignmentInspectionSuccess(inspection.getConsignment().getId());
                } catch (Exception ex) {
                    log.error("Failed to handle consignment success for inspection {}", inspectionId, ex);
                }
            }

        } catch (Exception e) {
            log.error("Error during inspection simulation", e);
            self.markInspectionFailedWithDetails(inspectionId, e);

            // Notify user of failure
            try {
                Inspection inspection = inspectionRepository.findById(inspectionId).orElseThrow();
                Long userId = null;
                if (inspection.getOrder() != null) {
                    userId = inspection.getOrder().getBuyer().getId();
                } else if (inspection.getConsignment() != null) {
                    userId = inspection.getConsignment().getSeller().getId();
                }

                if (userId != null) {
                    notificationService.createNotification(
                            userId,
                            "验货未通过",
                            "很遗憾，您的商品未通过验货。",
                            "INSPECTION_FAILED",
                            inspectionId);
                }
            } catch (Exception ex) {
                log.error("Failed to send inspection failure notification", ex);
            }

            // Handle Consignment Failure
            try {
                Inspection inspection = inspectionRepository.findById(inspectionId).orElseThrow();
                if (inspection.getConsignment() != null) {
                    self.handleConsignmentInspectionFailure(inspection.getConsignment().getId());
                }
            } catch (Exception ex) {
                log.error("Failed to handle consignment failure", ex);
            }
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
    public void markInspectionFailedWithDetails(Long inspectionId, Exception e) {
        Inspection inspection = inspectionRepository.findById(inspectionId).orElseThrow();
        inspection.setStatus("FAILED");

        String reason = mapFailureReason(e);
        String summary = "验货流程中断，系统已记录异常。未通过原因：" + reason;
        inspection.setResultSummary(summary);
        inspectionRepository.save(inspection);

        List<InspectionResult> results = inspectionResultRepository.findByInspectionId(inspectionId);
        for (InspectionResult result : results) {
            if ("PENDING".equals(result.getStatus()) || result.getStatus() == null) {
                result.setStatus("FAILED");
                result.setNotes("未完成检测：流程中断");
                result.setUpdatedAt(Instant.now());
            }
        }
        inspectionResultRepository.saveAll(results);
    }

    private String mapFailureReason(Exception e) {
        if (e == null || e.getMessage() == null || e.getMessage().isBlank()) {
            return "系统处理异常，请联系客服复检";
        }

        String message = e.getMessage();
        if (message.contains("Could not initialize proxy") || message.contains("no session")) {
            return "检测流程异常，请联系客服复检";
        }
        return message;
    }

    private String sanitizeSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return summary;
        }
        if (summary.contains("Could not initialize proxy") || summary.contains("no session")) {
            return "验货流程中断，系统已记录异常。未通过原因：检测流程异常，请联系客服复检";
        }
        return summary;
    }

    @Transactional
    public void handleConsignmentInspectionSuccess(Long consignmentId) {
        Consignment consignment = consignmentRepository.findById(consignmentId).orElseThrow();
        consignment.setStatus(ConsignmentStatus.PASSED);
        consignmentRepository.save(consignment);

        if (consignment.getProduct() != null) {
            productService.updateProductStatus(consignment.getProduct().getId(), ProductStatus.ON_SALE);
        }
    }

    @Transactional
    public void handleConsignmentInspectionFailure(Long consignmentId) {
        Consignment consignment = consignmentRepository.findById(consignmentId).orElseThrow();
        consignment.setStatus(ConsignmentStatus.REJECTED);
        consignmentRepository.save(consignment);

        if (consignment.getProduct() != null) {
            productService.updateProductStatus(consignment.getProduct().getId(), ProductStatus.OFF_SHELF);
        }
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
