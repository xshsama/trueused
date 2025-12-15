package com.xsh.trueused.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.dto.SellerStatsDTO;
import com.xsh.trueused.dto.UserDTO;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.enums.OrderStatus;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.mapper.UserMapper;
import com.xsh.trueused.repository.OrderRepository;
import com.xsh.trueused.repository.ProductRepository;
import com.xsh.trueused.repository.UserRepository;
import com.xsh.trueused.security.user.UserPrincipal;

import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UsersController {

        private final UserRepository userRepository;
        private final ProductRepository productRepository;
        private final OrderRepository orderRepository;

        public static record UpdateMeRequest(
                        @Size(max = 50) String nickname,
                        @Size(max = 255) String avatarUrl,
                        @Size(max = 300) String bio,
                        @Size(max = 20) String phone,
                        // 新增
                        @Size(max = 255) String coverImage,
                        @Size(max = 100) String location,
                        Boolean autoReplyEnabled,
                        @Size(max = 500) String autoReplyText,
                        Boolean vacationMode) {

        }

        @GetMapping("/me")
        public UserDTO me(@AuthenticationPrincipal UserPrincipal principal) {
                if (principal == null) {
                        throw new org.springframework.web.server.ResponseStatusException(
                                        org.springframework.http.HttpStatus.UNAUTHORIZED);
                }
                User user = userRepository.findById(principal.getId())
                                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.NOT_FOUND));
                return UserMapper.toDTO(user);
        }

        @PutMapping("/me")
        public UserDTO updateMe(@AuthenticationPrincipal UserPrincipal principal,
                        @RequestBody @Validated UpdateMeRequest req) {
                if (principal == null) {
                        throw new org.springframework.web.server.ResponseStatusException(
                                        org.springframework.http.HttpStatus.UNAUTHORIZED);
                }
                User user = userRepository.findById(principal.getId())
                                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.NOT_FOUND));

                if (req.nickname() != null) {
                        user.setNickname(req.nickname());
                }
                // 仅在非空白字符串时更新 avatarUrl，避免被空字符串覆盖
                if (req.avatarUrl() != null && !req.avatarUrl().isBlank()) {
                        user.setAvatarUrl(req.avatarUrl());
                }
                if (req.bio() != null) {
                        user.setBio(req.bio());
                }
                if (req.phone() != null) {
                        user.setPhone(req.phone());
                }
                if (req.coverImage() != null) {
                        user.setCoverImage(req.coverImage());
                }
                if (req.location() != null) {
                        user.setLocation(req.location());
                }
                if (req.autoReplyEnabled() != null) {
                        user.setAutoReplyEnabled(req.autoReplyEnabled());
                }
                if (req.autoReplyText() != null) {
                        user.setAutoReplyText(req.autoReplyText());
                }
                if (req.vacationMode() != null) {
                        user.setVacationMode(req.vacationMode());
                }

                User saved = userRepository.save(user);
                return UserMapper.toDTO(saved);
        }

        @GetMapping("/{id}/public-profile")
        public com.xsh.trueused.dto.PublicUserDTO getUserProfile(
                        @org.springframework.web.bind.annotation.PathVariable Long id) {
                User user = userRepository.findById(id)
                                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.NOT_FOUND));

                long sellingCount = productRepository.count((root, query, cb) -> cb.and(
                                cb.equal(root.get("seller").get("id"), id),
                                cb.equal(root.get("status"), ProductStatus.ON_SALE),
                                cb.equal(root.get("isDeleted"), false)));

                long soldCount = orderRepository.count((root, query, cb) -> cb.and(
                                cb.equal(root.get("seller").get("id"), id),
                                cb.equal(root.get("status"), OrderStatus.COMPLETED)));

                return new com.xsh.trueused.dto.PublicUserDTO(
                                user.getId(),
                                user.getUsername(),
                                user.getNickname(),
                                user.getAvatarUrl(),
                                user.getBio(),
                                user.getCoverImage(),
                                user.getLocation(),
                                user.getCreatedAt(),
                                (int) sellingCount,
                                (int) soldCount);
        }

        @GetMapping("/me/stats")
        public SellerStatsDTO getMyStats(@AuthenticationPrincipal UserPrincipal principal) {
                if (principal == null) {
                        throw new org.springframework.web.server.ResponseStatusException(
                                        org.springframework.http.HttpStatus.UNAUTHORIZED);
                }
                long sellerId = principal.getId();

                long onShelfProducts = productRepository.count((root, query, cb) -> cb.and(
                                cb.equal(root.get("seller").get("id"), sellerId),
                                cb.equal(root.get("status"), ProductStatus.ON_SALE),
                                cb.equal(root.get("isDeleted"), false)));

                long pendingOrders = orderRepository.count((root, query, cb) -> cb.and(
                                cb.equal(root.get("seller").get("id"), sellerId),
                                cb.equal(root.get("status"), OrderStatus.PAID))); // 待发货视为待处理

                long violationProducts = productRepository.count((root, query, cb) -> cb.and(
                                cb.equal(root.get("seller").get("id"), sellerId),
                                cb.equal(root.get("status"), ProductStatus.REJECTED),
                                cb.equal(root.get("isDeleted"), false))); // 暂时用下架代替违规

                return new SellerStatsDTO(onShelfProducts, pendingOrders, violationProducts);
        }
}
