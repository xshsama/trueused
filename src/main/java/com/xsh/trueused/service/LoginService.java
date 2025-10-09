package com.xsh.trueused.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.xsh.trueused.dto.LoginRequest;
import com.xsh.trueused.dto.LoginResponse;
import com.xsh.trueused.security.jwt.JwtTokenProvider;
import com.xsh.trueused.security.user.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 使用用户名密码认证，认证成功后签发 JWT 并返回。
     */
    public LoginResponse login(LoginRequest loginRequest) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(), loginRequest.getPassword());
        try {
            Authentication authentication = authenticationManager.authenticate(authToken);
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            String token = jwtTokenProvider.generateToken(authentication);
            long expiresInMs = 86400000L; // 与 JwtTokenProvider 默认一致，亦可从配置注入
            return new LoginResponse(
                    principal.getId(),
                    principal.getUsername(),
                    token,
                    expiresInMs,
                    principal.getAuthorities().stream().map(a -> a.getAuthority())
                            .collect(java.util.stream.Collectors.toSet()));
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        } catch (DisabledException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账户已禁用");
        } catch (LockedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账户已锁定");
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "认证失败");
        }
    }
}