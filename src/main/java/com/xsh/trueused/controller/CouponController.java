package com.xsh.trueused.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.api.ApiResponse;
import com.xsh.trueused.dto.CouponDTO;
import com.xsh.trueused.dto.UserCouponDTO;
import com.xsh.trueused.entity.Coupon;
import com.xsh.trueused.entity.UserCoupon;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.service.CouponService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public ApiResponse<List<Coupon>> getAvailableCoupons() {
        return ApiResponse.success(couponService.getAvailableCoupons());
    }

    @GetMapping("/my")
    public ApiResponse<List<UserCouponDTO>> getMyCoupons(@AuthenticationPrincipal UserPrincipal user) {
        List<UserCoupon> coupons = couponService.getMyCoupons(user.getId());
        List<UserCouponDTO> dtos = coupons.stream().map(this::toDTO).collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @PostMapping("/{id}/claim")
    public ApiResponse<Void> claimCoupon(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id) {
        couponService.claimCoupon(user.getId(), id);
        return ApiResponse.success(null);
    }

    @PostMapping("/my/{id}/use")
    public ApiResponse<Void> useCoupon(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id) {
        couponService.useCoupon(id, user.getId());
        return ApiResponse.success(null);
    }

    private UserCouponDTO toDTO(UserCoupon uc) {
        UserCouponDTO dto = new UserCouponDTO();
        dto.setId(uc.getId());
        dto.setCoupon(toCouponDTO(uc.getCoupon()));
        dto.setIsUsed(uc.getIsUsed());
        dto.setClaimedAt(uc.getClaimedAt());
        dto.setUsedAt(uc.getUsedAt());
        dto.setValidUntil(uc.getValidUntil());
        return dto;
    }

    private CouponDTO toCouponDTO(Coupon coupon) {
        if (coupon == null) {
            return null;
        }
        CouponDTO dto = new CouponDTO();
        dto.setId(coupon.getId());
        dto.setCode(coupon.getCode());
        dto.setTitle(coupon.getTitle());
        dto.setDescription(coupon.getDescription());
        dto.setType(coupon.getType());
        dto.setDiscountAmount(coupon.getDiscountAmount());
        dto.setMinSpend(coupon.getMinSpend());
        dto.setValidDays(coupon.getValidDays());
        dto.setIsActive(coupon.getIsActive());
        dto.setCreatedAt(coupon.getCreatedAt());
        dto.setUpdatedAt(coupon.getUpdatedAt());
        return dto;
    }
}
