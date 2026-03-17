package com.xsh.trueused.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import com.xsh.trueused.security.jwt.JwtTokenProvider;
import com.xsh.trueused.security.service.TokenRevocationService;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private TokenRevocationService tokenRevocationService;

    @InjectMocks
    private LoginService loginService;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldRejectRevokedRefreshToken() {
        when(tokenRevocationService.isRevoked("revoked-refresh-token")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> loginService.refreshAccessToken("revoked-refresh-token"));

        assertEquals(401, ex.getStatusCode().value());
        assertEquals("刷新令牌已失效", ex.getReason());
        verifyNoInteractions(jwtTokenProvider, userDetailsService);
    }

    @Test
    void shouldRevokePresentedTokensOnLogoutAndClearCookie() {
        loginService.logout("access-token", "refresh-token");

        verify(tokenRevocationService).revokeTokenIfActive("access-token", "LOGOUT");
        verify(tokenRevocationService).revokeTokenIfActive("refresh-token", "LOGOUT");
        String setCookie = response.getHeader("Set-Cookie");
        assertTrue(setCookie.contains("refresh_token="));
        assertTrue(setCookie.contains("Max-Age=0"));
    }
}
