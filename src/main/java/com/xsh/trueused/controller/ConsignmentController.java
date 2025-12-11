package com.xsh.trueused.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.dto.ConsignmentCreateRequest;
import com.xsh.trueused.dto.ConsignmentLogisticsRequest;
import com.xsh.trueused.dto.ConsignmentResponse;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.service.ConsignmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/consignments")
@RequiredArgsConstructor
public class ConsignmentController {

    private final ConsignmentService consignmentService;

    @PostMapping
    public ResponseEntity<ConsignmentResponse> createConsignment(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ConsignmentCreateRequest req) {
        return ResponseEntity.ok(consignmentService.createConsignment(req, user.getId()));
    }

    @GetMapping
    public ResponseEntity<List<ConsignmentResponse>> getMyConsignments(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(consignmentService.getMyConsignments(user.getId()));
    }

    @PutMapping("/{id}/logistics")
    public ResponseEntity<ConsignmentResponse> updateLogistics(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody ConsignmentLogisticsRequest req) {
        return ResponseEntity.ok(consignmentService.updateLogistics(id, req.getTrackingNo(), user.getId()));
    }

    // Internal/Admin endpoint to receive consignment
    // In a real app, this should be protected by role ADMIN or WAREHOUSE
    @PostMapping("/{id}/receive")
    public ResponseEntity<ConsignmentResponse> receiveConsignment(@PathVariable Long id) {
        return ResponseEntity.ok(consignmentService.receiveConsignment(id));
    }
}
