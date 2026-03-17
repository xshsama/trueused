package com.xsh.trueused.security.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.entity.RevokedToken;
import com.xsh.trueused.security.jwt.JwtTokenProvider;
import com.xsh.trueused.security.repository.RevokedTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public boolean isRevoked(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return revokedTokenRepository.existsByTokenHashAndExpiresAtAfter(hashToken(token), Instant.now());
    }

    @Transactional
    public boolean revokeTokenIfActive(String token, String reason) {
        if (token == null || token.isBlank() || !jwtTokenProvider.validateToken(token)) {
            return false;
        }

        String tokenHash = hashToken(token);
        if (revokedTokenRepository.existsByTokenHash(tokenHash)) {
            return false;
        }

        RevokedToken revokedToken = new RevokedToken();
        revokedToken.setTokenHash(tokenHash);
        revokedToken.setTokenType(jwtTokenProvider.getTokenType(token));
        revokedToken.setExpiresAt(jwtTokenProvider.getExpirationFromToken(token));
        revokedToken.setUserId(jwtTokenProvider.getUserIdFromToken(token));
        revokedToken.setReason(reason);

        try {
            revokedTokenRepository.save(revokedToken);
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
