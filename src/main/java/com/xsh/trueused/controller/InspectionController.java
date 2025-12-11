package com.xsh.trueused.controller;

import java.util.List;

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
import com.xsh.trueused.service.InspectionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

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
}
