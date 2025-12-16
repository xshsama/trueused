package com.xsh.trueused.service;

import java.math.BigDecimal;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.entity.User;
import com.xsh.trueused.entity.Wallet;
import com.xsh.trueused.entity.WalletTransaction;
import com.xsh.trueused.enums.WalletTransactionStatus;
import com.xsh.trueused.enums.WalletTransactionType;
import com.xsh.trueused.repository.UserRepository;
import com.xsh.trueused.repository.WalletRepository;
import com.xsh.trueused.repository.WalletTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
    public void setPayPassword(Long userId, String password) {
        Wallet wallet = getOrCreateWallet(userId);
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
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Top up amount must be positive");
        }
        Wallet wallet = getOrCreateWallet(userId);

        // Create transaction record
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(WalletTransactionType.TOP_UP);
        transaction.setStatus(WalletTransactionStatus.SUCCESS);
        transaction.setRemark("余额充值");
        transactionRepository.save(transaction);

        // Update balance
        int updatedRows = walletRepository.increaseBalance(wallet.getId(), amount);
        if (updatedRows == 0) {
            throw new RuntimeException("Failed to update balance");
        }
    }

    @Transactional
    public void payOrder(Long userId, Long orderId, BigDecimal amount, String password) {
        Wallet wallet = getOrCreateWallet(userId);

        if (!checkPayPassword(userId, password)) {
            throw new IllegalArgumentException("Invalid payment password");
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // Deduct balance
        int updatedRows = walletRepository.decreaseBalance(wallet.getId(), amount);
        if (updatedRows == 0) {
            throw new RuntimeException("Insufficient balance or concurrent update failed");
        }

        // Create transaction
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount.negate()); // Record as negative for payment? Or just positive with type PAYMENT?
        // Usually amount in DB is absolute, type determines sign. But let's keep it
        // positive and use type.
        transaction.setAmount(amount);
        transaction.setType(WalletTransactionType.PAYMENT);
        transaction.setOrderId(orderId);
        transaction.setStatus(WalletTransactionStatus.SUCCESS);
        transaction.setRemark("订单支付");
        transactionRepository.save(transaction);
    }

    @Transactional
    public void transferToSeller(Long sellerId, Long orderId, BigDecimal amount) {
        Wallet sellerWallet = getOrCreateWallet(sellerId);

        // Increase balance
        walletRepository.increaseBalance(sellerWallet.getId(), amount);

        // Create transaction
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(sellerWallet);
        transaction.setAmount(amount);
        transaction.setType(WalletTransactionType.INCOME);
        transaction.setOrderId(orderId);
        transaction.setStatus(WalletTransactionStatus.SUCCESS);
        transaction.setRemark("订单收入");
        transactionRepository.save(transaction);
    }

    @Transactional
    public void refund(Long userId, Long orderId, BigDecimal amount) {
        Wallet wallet = getOrCreateWallet(userId);

        walletRepository.increaseBalance(wallet.getId(), amount);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(WalletTransactionType.REFUND);
        transaction.setOrderId(orderId);
        transaction.setStatus(WalletTransactionStatus.SUCCESS);
        transaction.setRemark("订单退款");
        transactionRepository.save(transaction);
    }
}
