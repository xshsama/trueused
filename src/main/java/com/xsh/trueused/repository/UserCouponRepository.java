package com.xsh.trueused.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.UserCoupon;
import com.xsh.trueused.enums.CouponType;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    List<UserCoupon> findByUserId(Long userId);

    long countByUserIdAndIsUsedFalse(Long userId);

    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);

    List<UserCoupon> findByUserIdAndCoupon_TypeAndIsUsedFalse(Long userId, CouponType type);
}
