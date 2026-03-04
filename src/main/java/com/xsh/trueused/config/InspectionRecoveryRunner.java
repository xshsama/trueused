package com.xsh.trueused.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.xsh.trueused.entity.Consignment;
import com.xsh.trueused.enums.ConsignmentStatus;
import com.xsh.trueused.repository.ConsignmentRepository;
import com.xsh.trueused.repository.InspectionRepository;
import com.xsh.trueused.service.InspectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InspectionRecoveryRunner implements ApplicationRunner {

    private final ConsignmentRepository consignmentRepository;
    private final InspectionRepository inspectionRepository;
    private final InspectionService inspectionService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Checking for stuck inspections...");

        List<Consignment> stuckConsignments = consignmentRepository.findByStatus(ConsignmentStatus.INSPECTING);
        if (stuckConsignments.isEmpty()) {
            log.info("No stuck inspections found.");
            return;
        }

        log.info("Found {} stuck consignments. Resuming inspection...", stuckConsignments.size());

        for (Consignment c : stuckConsignments) {
            inspectionRepository.findByConsignmentId(c.getId()).ifPresent(inspection -> {
                if ("PENDING".equals(inspection.getStatus()) || "IN_PROGRESS".equals(inspection.getStatus())) {
                    log.info("Resuming inspection for consignment {}", c.getId());
                    inspectionService.simulateInspectionProcess(inspection.getId());
                }
            });
        }
    }
}
