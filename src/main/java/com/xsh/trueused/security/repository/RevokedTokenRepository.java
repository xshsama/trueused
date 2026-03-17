package com.xsh.trueused.security.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xsh.trueused.entity.RevokedToken;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByTokenHash(String tokenHash);

    boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, Instant instant);
}
