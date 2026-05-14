package com.xsh.trueused.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.security.jwt.JwtAuthenticationFilter;
import com.xsh.trueused.security.jwt.JwtTokenProvider;
import com.xsh.trueused.security.service.TokenRevocationService;

import org.springframework.security.core.userdetails.UserDetailsService;

@WebMvcTest(controllers = SecurityConfigTest.TestRoutes.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @Test
    void shouldAllowAnonymousReadOnlyCatalogEndpoints() throws Exception {
        mockMvc.perform(get("/api/products/1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/api/coupons")).andExpect(status().isOk());
        mockMvc.perform(get("/api/users/1/public-profile")).andExpect(status().isOk());
        mockMvc.perform(get("/api/reviews/products/1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/comments/products/1")).andExpect(status().isOk());
    }

    @Test
    void shouldRequireAuthenticationForProductMutationsAndMyProducts() throws Exception {
        mockMvc.perform(get("/api/products/my")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/products")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldAllowAuthenticatedUserToMutateProductAndReadPrivateProductList() throws Exception {
        mockMvc.perform(get("/api/products/my")).andExpect(status().isOk());
        mockMvc.perform(post("/api/products")).andExpect(status().isOk());
    }

    @Test
    void shouldRequireAuthenticationForPrivateCouponEndpoints() throws Exception {
        mockMvc.perform(get("/api/coupons/my")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/coupons/1/claim")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireAuthenticationForPrivateUserAndOrderEndpoints() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/orders/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldAllowAuthenticatedUserForPrivateUserAndOrderEndpoints() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/1")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void shouldRejectNonAdminFromAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminEndpointsForAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isOk());
    }

    @RestController
    static class TestRoutes {
        @GetMapping("/api/products/{id}")
        Map<String, Long> product(@PathVariable Long id) {
            return Map.of("id", id);
        }

        @GetMapping("/api/products/my")
        Map<String, String> myProducts() {
            return Map.of("scope", "mine");
        }

        @PostMapping("/api/products")
        Map<String, String> createProduct() {
            return Map.of("status", "created");
        }

        @GetMapping("/api/categories")
        Map<String, String> categories() {
            return Map.of("scope", "public");
        }

        @GetMapping("/api/coupons")
        Map<String, String> coupons() {
            return Map.of("scope", "public");
        }

        @GetMapping("/api/coupons/my")
        Map<String, String> myCoupons() {
            return Map.of("scope", "mine");
        }

        @PostMapping("/api/coupons/{id}/claim")
        Map<String, Long> claimCoupon(@PathVariable Long id) {
            return Map.of("id", id);
        }

        @GetMapping("/api/users/{id}/public-profile")
        Map<String, Long> publicProfile(@PathVariable Long id) {
            return Map.of("id", id);
        }

        @GetMapping("/api/users/me")
        Map<String, String> me() {
            return Map.of("scope", "mine");
        }

        @GetMapping("/api/orders/{id}")
        Map<String, Long> order(@PathVariable Long id) {
            return Map.of("id", id);
        }

        @GetMapping("/api/reviews/products/{id}")
        Map<String, Long> productReviews(@PathVariable Long id) {
            return Map.of("id", id);
        }

        @GetMapping("/api/comments/products/{id}")
        Map<String, Long> productComments(@PathVariable Long id) {
            return Map.of("id", id);
        }

        @GetMapping("/api/admin/users")
        Map<String, String> adminUsers() {
            return Map.of("scope", "admin");
        }
    }
}
