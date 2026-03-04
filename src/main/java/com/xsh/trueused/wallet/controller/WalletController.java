package com.xsh.trueused.wallet.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.wallet.dto.WalletDTO;
import com.xsh.trueused.wallet.dto.WalletSetPasswordRequest;
import com.xsh.trueused.wallet.dto.WalletTopUpRequest;
import com.xsh.trueused.wallet.dto.WalletTransactionDTO;
import com.xsh.trueused.wallet.dto.WalletWithdrawRequest;
import com.xsh.trueused.entity.Wallet;
import com.xsh.trueused.entity.WalletTransaction;
import com.xsh.trueused.wallet.repository.WalletTransactionRepository;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.wallet.service.WalletService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Validated
public class WalletController {

    private final WalletService walletService;
    private final WalletTransactionRepository transactionRepository;

    @GetMapping
    public ResponseEntity<WalletDTO> getMyWallet(@AuthenticationPrincipal UserPrincipal principal) {
        Wallet wallet = walletService.getOrCreateWallet(principal.getId());
        WalletDTO dto = new WalletDTO();
        dto.setId(wallet.getId());
        dto.setBalance(wallet.getBalance());
        dto.setFrozenAmount(wallet.getFrozenAmount());
        dto.setHasPayPassword(wallet.getPayPassword() != null);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/set-password")
    public ResponseEntity<Void> setPayPassword(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody WalletSetPasswordRequest request) {
        walletService.setPayPassword(principal.getId(), request.getPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/top-up")
    public ResponseEntity<Void> topUp(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody WalletTopUpRequest request) {
        walletService.topUp(principal.getId(), request.getAmount(), request.getBizNo());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody WalletWithdrawRequest request) {
        walletService.withdraw(principal.getId(), request.getAmount(), request.getPassword(), request.getBizNo());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<WalletTransactionDTO>> getTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Wallet wallet = walletService.getOrCreateWallet(principal.getId());
        Page<WalletTransaction> transactions = transactionRepository.findByWalletId(wallet.getId(), pageable);

        Page<WalletTransactionDTO> dtos = transactions.map(t -> {
            WalletTransactionDTO dto = new WalletTransactionDTO();
            dto.setId(t.getId());
            dto.setWalletId(t.getWallet().getId());
            dto.setUserId(t.getWallet().getUser().getId());
            dto.setAmount(t.getAmount());
            dto.setType(t.getType());
            dto.setOrderId(t.getOrderId());
            dto.setBizNo(t.getBizNo());
            dto.setStatus(t.getStatus());
            dto.setRemark(t.getRemark());
            dto.setCreatedAt(t.getCreatedAt());
            return dto;
        });

        return ResponseEntity.ok(dtos);
    }
}
