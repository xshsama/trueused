package com.xsh.trueused.repository;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.Wallet;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE Wallet w SET w.balance = w.balance + :amount, w.version = w.version + 1 WHERE w.id = :walletId")
    int increaseBalance(@Param("walletId") Long walletId, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE Wallet w SET w.balance = w.balance - :amount, w.version = w.version + 1 WHERE w.id = :walletId AND w.balance >= :amount")
    int decreaseBalance(@Param("walletId") Long walletId, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE Wallet w SET w.balance = w.balance - :amount, w.frozenAmount = w.frozenAmount + :amount, w.version = w.version + 1 WHERE w.id = :walletId AND w.balance >= :amount")
    int freezeBalance(@Param("walletId") Long walletId, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE Wallet w SET w.frozenAmount = w.frozenAmount - :amount, w.version = w.version + 1 WHERE w.id = :walletId AND w.frozenAmount >= :amount")
    int unfreezeBalance(@Param("walletId") Long walletId, @Param("amount") BigDecimal amount);
}
