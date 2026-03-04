package com.xsh.trueused.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.WalletTransaction;
import com.xsh.trueused.enums.WalletTransactionStatus;
import com.xsh.trueused.enums.WalletTransactionType;

import jakarta.persistence.LockModeType;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWalletId(Long walletId);

    Page<WalletTransaction> findByWalletId(Long walletId, Pageable pageable);

    Page<WalletTransaction> findByTypeAndStatus(WalletTransactionType type, WalletTransactionStatus status, Pageable pageable);

    List<WalletTransaction> findTop20ByTypeAndStatusOrderByCreatedAtAsc(
            WalletTransactionType type,
            WalletTransactionStatus status);

    boolean existsByBizNo(String bizNo);

    Optional<WalletTransaction> findByBizNo(String bizNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM WalletTransaction t WHERE t.id = :id")
    Optional<WalletTransaction> findByIdForUpdate(@Param("id") Long id);
}
