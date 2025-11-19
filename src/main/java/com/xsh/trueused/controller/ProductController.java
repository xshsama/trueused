package com.xsh.trueused.controller;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.dto.ProductCreateRequest;
import com.xsh.trueused.dto.ProductDTO;
import com.xsh.trueused.dto.ProductUpdateRequest;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public Page<ProductDTO> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return productService.search(q, categoryId, priceMin, priceMax, sort, page, size);
    }

    @GetMapping("/{id}")
    public ProductDTO detail(@PathVariable Long id) {
        return productService.findOne(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ProductDTO create(@RequestBody @Valid ProductCreateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        return productService.create(req, principal.getId());
    }

    @PutMapping("/{id}")
    public ProductDTO update(@PathVariable Long id, @RequestBody @Valid ProductUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        try {
            return productService.update(id, req, principal.getId());
        } catch (SecurityException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        try {
            productService.delete(id, principal.getId());
        } catch (SecurityException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, e.getMessage());
        }
    }
}
