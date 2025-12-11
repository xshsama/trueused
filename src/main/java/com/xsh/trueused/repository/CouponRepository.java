package com.xsh.trueused.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.Coupon;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    List<Coupon> findByIsActiveTrue();
}
