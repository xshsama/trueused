package com.xsh.trueused.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.dto.ConsignmentCreateRequest;
import com.xsh.trueused.dto.ConsignmentResponse;
import com.xsh.trueused.dto.PublicUserDTO;
import com.xsh.trueused.entity.Category;
import com.xsh.trueused.entity.Consignment;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.enums.ConsignmentStatus;
import com.xsh.trueused.mapper.ProductMapper;
import com.xsh.trueused.repository.CategoryRepository;
import com.xsh.trueused.repository.ConsignmentRepository;
import com.xsh.trueused.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsignmentService {

    private final ConsignmentRepository consignmentRepository;
    private final UserRepository userRepository;
    private final InspectionService inspectionService;
    private final CategoryRepository categoryRepository;
    private final ApplicationContext applicationContext;

    @Transactional
    public ConsignmentResponse createConsignment(ConsignmentCreateRequest req, Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Consignment consignment = new Consignment();
        consignment.setSeller(seller);
        consignment.setTitle(req.getTitle());
        consignment.setDescription(req.getDescription());
        consignment.setExpectedPrice(req.getExpectedPrice());
        consignment.setShippingMethod(req.getShippingMethod());
        consignment.setTrackingNoInbound(req.getTrackingNoInbound());
        consignment.setStatus(ConsignmentStatus.CREATED);
        consignment.setCategory(category);

        consignment = consignmentRepository.save(consignment);
        return toDTO(consignment);
    }

    @Transactional
    public ConsignmentResponse updateLogistics(Long consignmentId, String trackingNo, Long sellerId) {
        Consignment consignment = consignmentRepository.findById(consignmentId)
                .orElseThrow(() -> new RuntimeException("Consignment not found"));

        if (!consignment.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (consignment.getStatus() != ConsignmentStatus.CREATED) {
            throw new RuntimeException("Cannot update logistics in current status");
        }

        consignment.setTrackingNoInbound(trackingNo);
        consignment.setStatus(ConsignmentStatus.SHIPPED);
        consignment = consignmentRepository.save(consignment);

        // Simulate shipping delay and auto-receive
        CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
            try {
                log.info("Auto-receiving consignment {}", consignmentId);
                ConsignmentService self = applicationContext.getBean(ConsignmentService.class);
                self.receiveConsignment(consignmentId);
            } catch (Exception e) {
                log.error("Error auto-receiving consignment", e);
            }
        });

        return toDTO(consignment);
    }

    @Transactional
    public ConsignmentResponse receiveConsignment(Long consignmentId) {
        Consignment consignment = consignmentRepository.findById(consignmentId)
                .orElseThrow(() -> new RuntimeException("Consignment not found"));

        if (consignment.getStatus() != ConsignmentStatus.SHIPPED) {
            throw new RuntimeException("Consignment must be SHIPPED to be received");
        }

        consignment.setStatus(ConsignmentStatus.RECEIVED);
        consignment = consignmentRepository.save(consignment);

        // Trigger Inspection
        inspectionService.triggerInspectionForConsignment(consignmentId);

        return toDTO(consignment);
    }

    @Transactional(readOnly = true)
    public List<ConsignmentResponse> getMyConsignments(Long sellerId) {
        return consignmentRepository.findBySellerId(sellerId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private ConsignmentResponse toDTO(Consignment c) {
        ConsignmentResponse dto = new ConsignmentResponse();
        dto.setId(c.getId());
        dto.setTitle(c.getTitle());
        dto.setDescription(c.getDescription());
        dto.setExpectedPrice(c.getExpectedPrice());
        dto.setShippingMethod(c.getShippingMethod());
        dto.setTrackingNoInbound(c.getTrackingNoInbound());
        dto.setStatus(c.getStatus());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());

        if (c.getSeller() != null) {
            PublicUserDTO userDto = new PublicUserDTO(
                    c.getSeller().getId(),
                    c.getSeller().getUsername(),
                    c.getSeller().getNickname(),
                    c.getSeller().getAvatarUrl(),
                    c.getSeller().getBio(),
                    c.getSeller().getCoverImage(),
                    c.getSeller().getLocation(),
                    c.getSeller().getCreatedAt(),
                    0, // sellingCount placeholder
                    0 // soldCount placeholder
            );
            dto.setSeller(userDto);
        }

        if (c.getProduct() != null) {
            dto.setProduct(ProductMapper.toDTO(c.getProduct()));
        }

        return dto;
    }
}
