package com.xsh.trueused.dto;

import java.util.Set;

public record LoginResponse(
                Long userId,
                String username,
                String token,
                long expiresInMs,
                Set<String> roles) {
}
