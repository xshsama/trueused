package com.xsh.trueused.coupon.dto;

import java.time.Instant;

import lombok.Data;

@Data
public class UserCouponDTO {
    private Long id;
    private CouponDTO coupon;
    private Boolean isUsed;
    private Instant claimedAt;
    private Instant usedAt;
    private Instant validUntil;
}
