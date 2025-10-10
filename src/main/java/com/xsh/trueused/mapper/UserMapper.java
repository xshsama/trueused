package com.xsh.trueused.mapper;

import java.util.stream.Collectors;

import com.xsh.trueused.dto.UserDTO;
import com.xsh.trueused.entity.Role;
import com.xsh.trueused.entity.User;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                user.getRoles().stream().map(Role::getName).map(Enum::name).collect(Collectors.toSet()));
    }
}
