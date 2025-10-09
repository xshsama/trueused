package com.xsh.trueused.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.dto.LoginRequest;
import com.xsh.trueused.dto.LoginResponse;
import com.xsh.trueused.dto.RegisterRequest;
import com.xsh.trueused.dto.UserDTO;
import com.xsh.trueused.service.LoginService;
import com.xsh.trueused.service.RegisterService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
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
}
