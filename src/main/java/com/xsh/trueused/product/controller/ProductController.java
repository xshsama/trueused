package com.xsh.trueused.product.controller;

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

import com.xsh.trueused.product.dto.ProductCreateRequest;
import com.xsh.trueused.product.dto.ProductDTO;
import com.xsh.trueused.product.dto.ProductUpdateRequest;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.interaction.service.BrowsingHistoryService;
import com.xsh.trueused.product.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {
    private final ProductService productService;
    private final BrowsingHistoryService browsingHistoryService;

    @GetMapping
    public Page<ProductDTO> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long excludeSellerId = principal != null ? principal.getId() : null;
        return productService.search(q, categoryId, priceMin, priceMax, sort, page, size, excludeSellerId,
                sellerId, status);
    }

    @GetMapping("/my")
    public Page<ProductDTO> myProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        return productService.findMyProducts(principal.getId(), q, status, page, size);
    }

    @GetMapping("/{id}")
    public ProductDTO detail(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        // Increment views asynchronously or synchronously
        productService.incrementViews(id);

        // Record browsing history if user is logged in
        if (principal != null) {
            browsingHistoryService.recordHistory(principal.getId(), id);
        }

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

    @PutMapping("/{id}/publish")
    public ProductDTO publish(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        try {
            return productService.publishProduct(id, principal.getId());
        } catch (SecurityException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @PutMapping("/{id}/hide")
    public ProductDTO hide(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        try {
            return productService.hideProduct(id, principal.getId());
        } catch (SecurityException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @PutMapping("/{id}/polish")
    public void polish(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        try {
            productService.polishProduct(id, principal.getId());
        } catch (SecurityException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
