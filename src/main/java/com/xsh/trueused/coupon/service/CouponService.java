package com.xsh.trueused.coupon.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.entity.Coupon;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.entity.UserCoupon;
import com.xsh.trueused.coupon.repository.CouponRepository;
import com.xsh.trueused.coupon.repository.UserCouponRepository;
import com.xsh.trueused.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;

    public List<Coupon> getAvailableCoupons() {
        return couponRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<UserCoupon> getMyCoupons(Long userId) {
        List<UserCoupon> coupons = userCouponRepository.findByUserId(userId);
        // Initialize lazy loaded coupon to avoid LazyInitializationException in
        // Controller
        coupons.forEach(c -> c.getCoupon().getTitle());
        return coupons;
    }

    @Transactional
    public void claimCoupon(Long userId, Long couponId) {
        // Check if already claimed
        if (userCouponRepository.findByUserIdAndCouponId(userId, couponId).isPresent()) {
            throw new RuntimeException("You have already claimed this coupon");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        if (!coupon.getIsActive()) {
            throw new RuntimeException("Coupon is not active");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUser(user);
        userCoupon.setCoupon(coupon);
        userCoupon.setClaimedAt(Instant.now());
        userCoupon.setValidUntil(Instant.now().plus(coupon.getValidDays(), ChronoUnit.DAYS));

        userCouponRepository.save(userCoupon);
    }

    @Transactional
    public void useCoupon(Long userCouponId, Long userId) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        if (!userCoupon.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (userCoupon.getIsUsed()) {
            throw new RuntimeException("Coupon already used");
        }

        if (userCoupon.getValidUntil() != null && userCoupon.getValidUntil().isBefore(Instant.now())) {
            throw new RuntimeException("Coupon expired");
        }

        userCoupon.setIsUsed(true);
        userCoupon.setUsedAt(Instant.now());
        userCouponRepository.save(userCoupon);
    }
}
