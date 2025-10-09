package com.xsh.trueused.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.dto.RegisterRequest;
import com.xsh.trueused.dto.UserDTO;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.enums.UserStatus;
import com.xsh.trueused.mapper.UserMapper;
import com.xsh.trueused.repository.RoleRepository;
import com.xsh.trueused.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegisterService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDTO register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("邮箱已存在");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        roleRepository.findByName("ROLE_USER").ifPresent(role -> user.getRoles().add(role));
        User saved = userRepository.save(user);
        return UserMapper.toDTO(saved);
    }
}
