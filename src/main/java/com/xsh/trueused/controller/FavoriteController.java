package com.xsh.trueused.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.dto.FavoriteDTO;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.service.FavoriteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    @PostMapping("/products/{productId}")
    public void add(@PathVariable Long productId, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        favoriteService.add(productId, principal.getId());
    }

    @DeleteMapping("/products/{productId}")
    public void remove(@PathVariable Long productId, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        favoriteService.remove(productId, principal.getId());
    }

    @GetMapping
    public Page<FavoriteDTO> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        return favoriteService.list(principal.getId(), page, size);
    }
}
