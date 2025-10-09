package com.xsh.trueused.dto;

import java.util.Set;

import com.xsh.trueused.enums.UserStatus;

public record UserDTO(
        Long id,
        String username,
        String email,
        UserStatus status,
        Set<String> roles) {
}
