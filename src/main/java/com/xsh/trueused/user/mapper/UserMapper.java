package com.xsh.trueused.user.mapper;

import java.util.stream.Collectors;

import com.xsh.trueused.user.dto.UserDTO;
import com.xsh.trueused.entity.Role;
import com.xsh.trueused.entity.User;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserDTO toDTO(User user) {
        return toDTO(user, 0);
    }

    public static UserDTO toDTO(User user, Integer couponCount) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                user.getRoles().stream().map(Role::getName).map(Enum::name).collect(Collectors.toSet()),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getPhone(),
                user.getEmailVerified(),
                user.getPhoneVerified(),
                user.getLastLoginAt(),
                user.getBanReason(),
                user.getBanUntil(),
                user.getCoverImage(),
                user.getLocation(),
                user.getAutoReplyEnabled(),
                user.getAutoReplyText(),
                user.getVacationMode(),
                couponCount);
    }
}
