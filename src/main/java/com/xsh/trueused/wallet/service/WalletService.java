package com.xsh.trueused.wallet.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import com.xsh.trueused.wallet.dto.WalletTransactionDTO;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.entity.Wallet;
import com.xsh.trueused.entity.WalletTransaction;
import com.xsh.trueused.enums.WalletTransactionStatus;
import com.xsh.trueused.enums.WalletTransactionType;
import com.xsh.trueused.user.repository.UserRepository;
import com.xsh.trueused.wallet.repository.WalletRepository;
import com.xsh.trueused.wallet.repository.WalletTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletService {
    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    Wallet wallet = new Wallet();
                    wallet.setUser(user);
                    return walletRepository.save(wallet);
                });
    }

    public Wallet getWallet(Long userId) {
        return getOrCreateWallet(userId);
    }

    @Transactional
    protected Wallet getOrCreateWalletForUpdate(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(wallet -> walletRepository.findByIdForUpdate(wallet.getId())
                        .orElseThrow(() -> new RuntimeException("Wallet not found for update")))
                .orElseGet(() -> createWallet(userId));
    }

    private Wallet createWallet(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        return walletRepository.save(wallet);
    }

    private WalletTransaction buildTransaction(Wallet wallet, BigDecimal amount,
            WalletTransactionType type, WalletTransactionStatus status,
            Long orderId, String bizNo, String remark) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setOrderId(orderId);
        transaction.setBizNo(bizNo);
        transaction.setRemark(remark);
        return transaction;
    }

    @Transactional
    public void setPayPassword(Long userId, String password) {
        Wallet wallet = getOrCreateWalletForUpdate(userId);
        wallet.setPayPassword(passwordEncoder.encode(password));
        walletRepository.save(wallet);
    }

    public boolean checkPayPassword(Long userId, String password) {
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getPayPassword() == null) {
            return false;
        }
        return passwordEncoder.matches(password, wallet.getPayPassword());
    }

    @Transactional
    public void topUp(Long userId, BigDecimal amount) {
        topUp(userId, amount, null);
    }

    @Transactional
    public void topUp(Long userId, BigDecimal amount, String bizNo) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Top up amount must be positive");
        }
        String finalBizNo = StringUtils.hasText(bizNo)
                ? bizNo.trim()
                : "TOPUP:" + userId + ":" + System.currentTimeMillis();

        if (transactionRepository.existsByBizNo(finalBizNo)) {
            return;
        }

        Wallet wallet = getOrCreateWalletForUpdate(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = buildTransaction(
                wallet, amount, WalletTransactionType.TOP_UP, WalletTransactionStatus.SUCCESS,
                null, finalBizNo, "余额充值");
        transactionRepository.save(transaction);
    }

    @Transactional
    public void payOrder(Long userId, Long orderId, BigDecimal amount, String password) {
        String paymentBizNo = "ORDER_PAY:" + orderId;
        if (transactionRepository.existsByBizNo(paymentBizNo)) {
            return;
        }

        Wallet wallet = getOrCreateWalletForUpdate(userId);
        if (wallet.getPayPassword() == null || !passwordEncoder.matches(password, wallet.getPayPassword())) {
            throw new IllegalArgumentException("Invalid payment password");
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        // 支付阶段只冻结买家资金：available -= amount, frozen += amount
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setFrozenAmount(wallet.getFrozenAmount().add(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = buildTransaction(
                wallet, amount, WalletTransactionType.PAYMENT, WalletTransactionStatus.SUCCESS,
                orderId, paymentBizNo, "订单支付(冻结资金)");
        transactionRepository.save(transaction);
    }

    @Transactional
    public void transferToSeller(Long sellerId, Long buyerId, Long orderId, BigDecimal amount) {
        String buyerReleaseBizNo = "ORDER_SETTLE_BUYER:" + orderId;
        String sellerIncomeBizNo = "ORDER_SETTLE_SELLER:" + orderId;
        if (transactionRepository.existsByBizNo(buyerReleaseBizNo)
                && transactionRepository.existsByBizNo(sellerIncomeBizNo)) {
            return;
        }

        Long firstUserId = Math.min(sellerId, buyerId);
        Long secondUserId = Math.max(sellerId, buyerId);

        Wallet firstWallet = getOrCreateWalletForUpdate(firstUserId);
        Wallet secondWallet = Objects.equals(firstUserId, secondUserId)
                ? firstWallet
                : getOrCreateWalletForUpdate(secondUserId);

        Wallet buyerWallet = Objects.equals(firstUserId, buyerId) ? firstWallet : secondWallet;
        Wallet sellerWallet = Objects.equals(firstUserId, sellerId) ? firstWallet : secondWallet;

        if (!transactionRepository.existsByBizNo(buyerReleaseBizNo)) {
            boolean releasedFromFrozen = buyerWallet.getFrozenAmount().compareTo(amount) >= 0;
            if (releasedFromFrozen) {
                buyerWallet.setFrozenAmount(buyerWallet.getFrozenAmount().subtract(amount));
                walletRepository.save(buyerWallet);
            }
            transactionRepository.save(buildTransaction(
                    buyerWallet, amount, WalletTransactionType.PAYMENT, WalletTransactionStatus.SUCCESS,
                    orderId, buyerReleaseBizNo,
                    releasedFromFrozen ? "订单结算(冻结资金扣减)" : "订单结算(兼容旧数据: 无冻结资金可扣减)"));
        }

        if (!transactionRepository.existsByBizNo(sellerIncomeBizNo)) {
            sellerWallet.setBalance(sellerWallet.getBalance().add(amount));
            walletRepository.save(sellerWallet);
            transactionRepository.save(buildTransaction(
                    sellerWallet, amount, WalletTransactionType.INCOME, WalletTransactionStatus.SUCCESS,
                    orderId, sellerIncomeBizNo, "订单收入"));
        }
    }

    @Transactional
    public void refund(Long userId, Long orderId, BigDecimal amount) {
        String refundBizNo = "ORDER_REFUND:" + orderId;
        if (transactionRepository.existsByBizNo(refundBizNo)) {
            return;
        }

        Wallet wallet = getOrCreateWalletForUpdate(userId);

        // 优先从冻结资金释放到可用余额；若冻结不足，兼容历史数据直接加余额。
        if (wallet.getFrozenAmount().compareTo(amount) >= 0) {
            wallet.setFrozenAmount(wallet.getFrozenAmount().subtract(amount));
            wallet.setBalance(wallet.getBalance().add(amount));
        } else {
            wallet.setBalance(wallet.getBalance().add(amount));
        }
        walletRepository.save(wallet);

        WalletTransaction transaction = buildTransaction(
                wallet, amount, WalletTransactionType.REFUND, WalletTransactionStatus.SUCCESS,
                orderId, refundBizNo, "订单退款");
        transactionRepository.save(transaction);
    }

    @Transactional
    public void withdraw(Long userId, BigDecimal amount, String password, String bizNo) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdraw amount must be positive");
        }

        String finalBizNo;
        if (StringUtils.hasText(bizNo)) {
            String trimmedBizNo = bizNo.trim();
            finalBizNo = trimmedBizNo.startsWith("WITHDRAW:")
                    ? trimmedBizNo
                    : "WITHDRAW:" + trimmedBizNo;
        } else {
            finalBizNo = "WITHDRAW:" + userId + ":" + System.currentTimeMillis();
        }
        if (transactionRepository.existsByBizNo(finalBizNo)) {
            return;
        }

        Wallet wallet = getOrCreateWalletForUpdate(userId);
        if (wallet.getPayPassword() == null || !passwordEncoder.matches(password, wallet.getPayPassword())) {
            throw new IllegalArgumentException("Invalid payment password");
        }
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setFrozenAmount(wallet.getFrozenAmount().add(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = buildTransaction(
                wallet, amount, WalletTransactionType.WITHDRAWAL, WalletTransactionStatus.PENDING,
                null, finalBizNo, "提现申请(待审核)");
        transactionRepository.save(transaction);

        // 无管理端场景下，提交后立即走一次自动审核，避免前端长期停留在待审核状态。
        try {
            approveWithdrawal(transaction.getId());
        } catch (Exception ex) {
            log.warn("Immediate auto-approve failed for withdrawal tx={}", transaction.getId(), ex);
        }
    }

    @Transactional(readOnly = true)
    public Page<WalletTransaction> getPendingWithdrawals(Pageable pageable) {
        return transactionRepository.findByTypeAndStatus(
                WalletTransactionType.WITHDRAWAL,
                WalletTransactionStatus.PENDING,
                pageable);
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionDTO> getPendingWithdrawalDTOs(Pageable pageable) {
        return getPendingWithdrawals(pageable).map(this::toDTO);
    }

    @Transactional
    public void approveWithdrawal(Long transactionId) {
        WalletTransaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new RuntimeException("Withdrawal transaction not found"));
        if (transaction.getType() != WalletTransactionType.WITHDRAWAL) {
            throw new IllegalArgumentException("Transaction is not a withdrawal");
        }

        if (transaction.getStatus() == WalletTransactionStatus.SUCCESS) {
            return;
        }
        if (transaction.getStatus() == WalletTransactionStatus.FAILED) {
            throw new RuntimeException("Withdrawal transaction is already failed");
        }

        Wallet wallet = walletRepository.findByIdForUpdate(transaction.getWallet().getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        if (wallet.getFrozenAmount().compareTo(transaction.getAmount()) < 0) {
            throw new RuntimeException("Frozen balance is insufficient for withdrawal approval");
        }
        wallet.setFrozenAmount(wallet.getFrozenAmount().subtract(transaction.getAmount()));
        walletRepository.save(wallet);

        transaction.setStatus(WalletTransactionStatus.SUCCESS);
        transaction.setRemark("提现审核通过");
        transactionRepository.save(transaction);
    }

    @Transactional
    public void rejectWithdrawal(Long transactionId, String reason) {
        WalletTransaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new RuntimeException("Withdrawal transaction not found"));
        if (transaction.getType() != WalletTransactionType.WITHDRAWAL) {
            throw new IllegalArgumentException("Transaction is not a withdrawal");
        }

        if (transaction.getStatus() == WalletTransactionStatus.FAILED) {
            return;
        }
        if (transaction.getStatus() == WalletTransactionStatus.SUCCESS) {
            throw new RuntimeException("Withdrawal transaction is already approved");
        }

        Wallet wallet = walletRepository.findByIdForUpdate(transaction.getWallet().getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        if (wallet.getFrozenAmount().compareTo(transaction.getAmount()) >= 0) {
            wallet.setFrozenAmount(wallet.getFrozenAmount().subtract(transaction.getAmount()));
        }
        wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
        walletRepository.save(wallet);

        transaction.setStatus(WalletTransactionStatus.FAILED);
        if (StringUtils.hasText(reason)) {
            transaction.setRemark("提现驳回: " + reason.trim());
        } else {
            transaction.setRemark("提现驳回");
        }
        transactionRepository.save(transaction);
    }

    /**
     * 假审核：无管理端时自动通过提现申请（每10秒处理一批）。
     */
    @Scheduled(fixedDelay = 10000)
    public void autoApprovePendingWithdrawals() {
        List<WalletTransaction> pending = transactionRepository.findTop20ByTypeAndStatusOrderByCreatedAtAsc(
                WalletTransactionType.WITHDRAWAL,
                WalletTransactionStatus.PENDING);
        for (WalletTransaction tx : pending) {
            try {
                transactionTemplate.executeWithoutResult(status -> approveWithdrawal(tx.getId()));
            } catch (Exception ex) {
                // 单条失败不阻塞后续处理
                log.warn("Scheduled auto-approve failed for withdrawal tx={}", tx.getId(), ex);
            }
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpPendingWithdrawalsOnStartup() {
        autoApprovePendingWithdrawals();
    }

    private WalletTransactionDTO toDTO(WalletTransaction t) {
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
    }
}
