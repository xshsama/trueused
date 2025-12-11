package com.xsh.trueused.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.UserCoupon;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    List<UserCoupon> findByUserId(Long userId);

    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);
}
