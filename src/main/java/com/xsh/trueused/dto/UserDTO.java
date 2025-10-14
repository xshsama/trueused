package com.xsh.trueused.dto;

import java.time.Instant;
import java.util.Set;

import com.xsh.trueused.enums.UserStatus;

public record UserDTO(
                Long id,
                String username,
                String email,
                UserStatus status,
                Set<String> roles,
                String nickname,
                String avatarUrl,
                String bio,
                String phone,
                Boolean emailVerified,
                Boolean phoneVerified,
                Instant lastLoginAt,
                String banReason,
                Instant banUntil) {
}
