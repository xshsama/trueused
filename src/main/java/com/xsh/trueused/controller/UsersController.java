package com.xsh.trueused.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.dto.UserDTO;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.mapper.UserMapper;
import com.xsh.trueused.repository.UserRepository;
import com.xsh.trueused.security.user.UserPrincipal;

import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UsersController {

    private final UserRepository userRepository;

    public static record UpdateMeRequest(
            @Size(max = 50) String nickname,
            @Size(max = 255) String avatarUrl,
            @Size(max = 300) String bio,
            @Size(max = 20) String phone) {
    }

    @GetMapping("/me")
    public UserDTO me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        return UserMapper.toDTO(user);
    }

    @PutMapping("/me")
    public UserDTO updateMe(@AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Validated UpdateMeRequest req) {
        if (principal == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));

        if (req.nickname() != null) {
            user.setNickname(req.nickname());
        }
        // 仅在非空白字符串时更新 avatarUrl，避免被空字符串覆盖
        if (req.avatarUrl() != null && !req.avatarUrl().isBlank()) {
            user.setAvatarUrl(req.avatarUrl());
        }
        if (req.bio() != null) {
            user.setBio(req.bio());
        }
        if (req.phone() != null) {
            user.setPhone(req.phone());
        }

        User saved = userRepository.save(user);
        return UserMapper.toDTO(saved);
    }
}
