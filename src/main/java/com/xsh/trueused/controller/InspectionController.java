package com.xsh.trueused.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.api.ApiResponse;
import com.xsh.trueused.dto.InspectionFlowDTO;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.service.InspectionPdfService;
import com.xsh.trueused.service.InspectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class InspectionController {

    private final InspectionService inspectionService;
    private final InspectionPdfService inspectionPdfService;

    @PostMapping("/platform/receive_package")
    public ApiResponse<InspectionFlowDTO> receivePackage(@RequestParam Long orderId) {
        return ApiResponse.success(inspectionService.triggerInspection(orderId));
    }

    @GetMapping("/inspections/{orderId}/flow")
    public ApiResponse<InspectionFlowDTO> getInspectionFlow(@PathVariable Long orderId) {
        return ApiResponse.success(inspectionService.getInspectionFlow(orderId));
    }

    @GetMapping("/inspections/consignment/{consignmentId}/flow")
    public ApiResponse<InspectionFlowDTO> getConsignmentInspectionFlow(@PathVariable Long consignmentId) {
        return ApiResponse.success(inspectionService.getInspectionFlowByConsignment(consignmentId));
    }

    @GetMapping("/inspections/my")
    public ApiResponse<List<InspectionFlowDTO>> getMyInspections(@AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.success(inspectionService.getMyInspections(user.getId()));
    }

    @GetMapping(value = "/inspections/{id}/pdf")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long id) {
        log.info("Request received to generate PDF for inspection ID: {}", id);
        try {
            InspectionFlowDTO inspection = inspectionService.getInspectionById(id);
            log.info("Found inspection data for ID: {}, Product: {}", id, inspection.getProductTitle());

            byte[] pdfBytes = inspectionPdfService.generateInspectionReportPdf(inspection);
            log.info("PDF stream generated successfully for inspection ID: {}, size: {} bytes", id, pdfBytes.length);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "inline; filename=inspection-report-" + id + ".pdf");
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Failed to generate PDF for inspection ID: {}", id, e);
            // Return error message as plain text if something goes wrong, or letting the global handler handle it.
            // Returning a 500 with the error message in body.
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("PDF Generation Error: " + e.getMessage()).getBytes());
        }
    }
}
