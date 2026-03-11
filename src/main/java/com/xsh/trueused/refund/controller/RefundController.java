package com.xsh.trueused.refund.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.refund.dto.RefundRequestCreateDTO;
import com.xsh.trueused.refund.dto.RefundRequestDTO;
import com.xsh.trueused.entity.RefundRequest;
import com.xsh.trueused.refund.mapper.RefundRequestMapper;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.refund.service.RefundService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class RefundController {

    @Autowired
    private RefundService refundService;

    @Autowired
    private RefundRequestMapper refundRequestMapper;

    @GetMapping("/{id}/refund-detail")
    public ResponseEntity<RefundRequestDTO> getRefundDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        RefundRequest refundRequest = refundService.getRefundRequestByOrderId(id, currentUser.getId());
        return ResponseEntity.ok(refundRequestMapper.toDTO(refundRequest));
    }

    @PostMapping("/{id}/refund-request")
    public ResponseEntity<RefundRequestDTO> requestRefund(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequestCreateDTO dto,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        RefundRequest refundRequest = refundService.requestRefund(id, dto, currentUser.getId());
        return ResponseEntity.ok(refundRequestMapper.toDTO(refundRequest));
    }

    @PutMapping("/{id}/refund-approve")
    public ResponseEntity<RefundRequestDTO> approveRefund(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        RefundRequest refundRequest = refundService.approveRefund(id, currentUser.getId());
        return ResponseEntity.ok(refundRequestMapper.toDTO(refundRequest));
    }

    @PutMapping("/{id}/refund-reject")
    public ResponseEntity<RefundRequestDTO> rejectRefund(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        RefundRequest refundRequest = refundService.rejectRefund(id, currentUser.getId());
        return ResponseEntity.ok(refundRequestMapper.toDTO(refundRequest));
    }

    // refund-complete usually called by system or after return shipping confirmed
    // For now, maybe we don't expose it directly or only for admin/seller after
    // return?
    // The plan says: PUT /api/orders/{id}/refund-complete
    @PutMapping("/{id}/refund-complete")
    public ResponseEntity<RefundRequestDTO> completeRefund(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        RefundRequest refundRequest = refundService.completeRefund(id, currentUser.getId());
        return ResponseEntity.ok(refundRequestMapper.toDTO(refundRequest));
    }
}
