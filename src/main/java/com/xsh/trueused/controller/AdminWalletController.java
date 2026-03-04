package com.xsh.trueused.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.dto.WalletTransactionDTO;
import com.xsh.trueused.service.WalletService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/wallet")
@RequiredArgsConstructor
public class AdminWalletController {

    private final WalletService walletService;

    @GetMapping("/withdrawals/pending")
    public ResponseEntity<Page<WalletTransactionDTO>> getPendingWithdrawals(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<WalletTransactionDTO> dtos = walletService.getPendingWithdrawalDTOs(pageable);
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/withdrawals/{transactionId}/approve")
    public ResponseEntity<Void> approveWithdrawal(@PathVariable Long transactionId) {
        walletService.approveWithdrawal(transactionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/withdrawals/{transactionId}/reject")
    public ResponseEntity<Void> rejectWithdrawal(@PathVariable Long transactionId,
            @RequestBody(required = false) Map<String, Object> request) {
        String reason = null;
        if (request != null) {
            Object reasonValue = request.get("reason");
            if (reasonValue != null) {
                reason = String.valueOf(reasonValue);
            }
        }
        walletService.rejectWithdrawal(transactionId, reason);
        return ResponseEntity.ok().build();
    }

}
