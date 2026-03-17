package com.xsh.trueused.auth.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;

import com.xsh.trueused.auth.dto.LoginRequest;
import com.xsh.trueused.auth.dto.LoginResponse;
import com.xsh.trueused.auth.dto.RegisterRequest;
import com.xsh.trueused.user.dto.UserDTO;
import com.xsh.trueused.auth.service.LoginService;
import com.xsh.trueused.auth.service.RegisterService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    private final LoginService loginService;
    private final RegisterService registerService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Validated LoginRequest loginRequest) {
        return loginService.login(loginRequest);
    }

    @PostMapping("/register")
    public UserDTO register(@RequestBody @Validated RegisterRequest request) {
        return registerService.register(request);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        return loginService.refreshAccessToken(refreshToken);
    }

    @PostMapping("/logout")
    public void logout(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        String accessToken = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            accessToken = authorizationHeader.substring(7);
        }
        loginService.logout(accessToken, refreshToken);
    }
}
