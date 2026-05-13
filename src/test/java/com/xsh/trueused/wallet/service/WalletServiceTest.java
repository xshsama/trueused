package com.xsh.trueused.wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import com.xsh.trueused.entity.User;
import com.xsh.trueused.entity.Wallet;
import com.xsh.trueused.entity.WalletTransaction;
import com.xsh.trueused.enums.WalletTransactionStatus;
import com.xsh.trueused.enums.WalletTransactionType;
import com.xsh.trueused.user.repository.UserRepository;
import com.xsh.trueused.wallet.repository.WalletRepository;
import com.xsh.trueused.wallet.repository.WalletTransactionRepository;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TransactionTemplate transactionTemplate;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(
                walletRepository,
                transactionRepository,
                userRepository,
                passwordEncoder,
                transactionTemplate);
    }

    @Test
    void payOrderShouldFreezeBuyerBalanceAndCreatePaymentTransaction() {
        Wallet buyerWallet = wallet(1L, 100L, "encoded-password", "100.00", "0.00");
        when(transactionRepository.existsByBizNo("ORDER_PAY:900")).thenReturn(false);
        mockExistingWalletForUpdate(100L, buyerWallet);
        when(passwordEncoder.matches("123456", "encoded-password")).thenReturn(true);

        walletService.payOrder(100L, 900L, amount("45.50"), "123456");

        assertMoney("54.50", buyerWallet.getBalance());
        assertMoney("45.50", buyerWallet.getFrozenAmount());

        ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        WalletTransaction tx = txCaptor.getValue();
        assertEquals(WalletTransactionType.PAYMENT, tx.getType());
        assertEquals(WalletTransactionStatus.SUCCESS, tx.getStatus());
        assertEquals("ORDER_PAY:900", tx.getBizNo());
        assertMoney("45.50", tx.getAmount());
    }

    @Test
    void payOrderShouldBeIdempotentByBusinessNumber() {
        when(transactionRepository.existsByBizNo("ORDER_PAY:900")).thenReturn(true);

        walletService.payOrder(100L, 900L, amount("45.50"), "123456");

        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).findByIdForUpdate(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferToSellerShouldReleaseBuyerFrozenAmountAndCreditSeller() {
        Wallet buyerWallet = wallet(1L, 100L, null, "10.00", "80.00");
        Wallet sellerWallet = wallet(2L, 200L, null, "20.00", "0.00");
        mockExistingWalletForUpdate(100L, buyerWallet);
        mockExistingWalletForUpdate(200L, sellerWallet);

        walletService.transferToSeller(200L, 100L, 900L, amount("80.00"));

        assertMoney("10.00", buyerWallet.getBalance());
        assertMoney("0.00", buyerWallet.getFrozenAmount());
        assertMoney("100.00", sellerWallet.getBalance());

        ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactionRepository, org.mockito.Mockito.times(2)).save(txCaptor.capture());
        assertEquals("ORDER_SETTLE_BUYER:900", txCaptor.getAllValues().get(0).getBizNo());
        assertEquals(WalletTransactionType.INCOME, txCaptor.getAllValues().get(1).getType());
        assertEquals("ORDER_SETTLE_SELLER:900", txCaptor.getAllValues().get(1).getBizNo());
    }

    @Test
    void refundShouldReleaseFrozenAmountBackToBalance() {
        Wallet buyerWallet = wallet(1L, 100L, null, "10.00", "45.50");
        when(transactionRepository.existsByBizNo("ORDER_REFUND:900")).thenReturn(false);
        mockExistingWalletForUpdate(100L, buyerWallet);

        walletService.refund(100L, 900L, amount("45.50"));

        assertMoney("55.50", buyerWallet.getBalance());
        assertMoney("0.00", buyerWallet.getFrozenAmount());

        ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertEquals(WalletTransactionType.REFUND, txCaptor.getValue().getType());
        assertEquals("ORDER_REFUND:900", txCaptor.getValue().getBizNo());
    }

    private static Wallet wallet(Long walletId, Long userId, String payPassword, String balance, String frozenAmount) {
        User user = new User();
        user.setId(userId);

        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setUser(user);
        wallet.setPayPassword(payPassword);
        wallet.setBalance(amount(balance));
        wallet.setFrozenAmount(amount(frozenAmount));
        return wallet;
    }

    private void mockExistingWalletForUpdate(Long userId, Wallet wallet) {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.findByIdForUpdate(wallet.getId())).thenReturn(Optional.of(wallet));
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, amount(expected).compareTo(actual));
    }
}
