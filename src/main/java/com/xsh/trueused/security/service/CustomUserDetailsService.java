package com.xsh.trueused.security.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.entity.User;
import com.xsh.trueused.repository.UserRepository;
import com.xsh.trueused.security.user.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        System.out.println("DEBUG: Loaded user: " + user.getUsername());
        System.out.println("DEBUG: User ID: " + user.getId());
        if (user.getId() == null) {
            System.err.println("ERROR: User ID is NULL from database!");
        }
        System.out.println("DEBUG: Creating UserPrincipal: " + UserPrincipal.from(user).toString());
        return UserPrincipal.from(user);
        // --- 添加调试日志 ---

    }
}
