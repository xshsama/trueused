package com.xsh.trueused.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    private final org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

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
            long expiresInMs = jwtTokenProvider.getAccessTokenExpirationMs();

            // 生成刷新令牌，写入 HttpOnly Cookie（安全属性按需求配置）
            String refreshToken = jwtTokenProvider.generateRefreshTokenFromUser(principal);
            ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                    .httpOnly(true)
                    .secure(false) // 如果是 https，改为 true
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(jwtTokenProvider.getRefreshTokenExpirationMs() / 1000)
                    .build();
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null && attrs.getResponse() != null) {
                attrs.getResponse().addHeader("Set-Cookie", cookie.toString());
            }
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

    /**
     * 使用 refresh_token 刷新 Access Token。
     */
    public LoginResponse refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少刷新令牌");
        }
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "刷新令牌无效");
        }
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        // 重新加载用户，补足 id 与角色
        org.springframework.security.core.userdetails.UserDetails userDetails = userDetailsService
                .loadUserByUsername(username);
        UserPrincipal principal = (userDetails instanceof UserPrincipal)
                ? (UserPrincipal) userDetails
                : new UserPrincipal(null, userDetails.getUsername(), null, "", userDetails.isEnabled(),
                        new java.util.HashSet<>(userDetails.getAuthorities()));

        String accessToken = jwtTokenProvider.generateAccessTokenFromPrincipal(principal);
        long expiresInMs = jwtTokenProvider.getAccessTokenExpirationMs();

        // 刷新 refresh_token（可选：轮换刷新令牌）
        String newRefreshToken = jwtTokenProvider.generateRefreshTokenFromUser(userDetails);
        ResponseCookie cookie = ResponseCookie.from("refresh_token", newRefreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtTokenProvider.getRefreshTokenExpirationMs() / 1000)
                .build();
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null && attrs.getResponse() != null) {
            attrs.getResponse().addHeader("Set-Cookie", cookie.toString());
        }

        java.util.Set<String> roles = userDetails.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());

        return new LoginResponse(
                principal.getId(),
                username,
                accessToken,
                expiresInMs,
                roles);
    }
}