package com.xsh.trueused.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.dto.FavoriteDTO;
import com.xsh.trueused.entity.Favorite;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.mapper.FavoriteMapper;
import com.xsh.trueused.repository.FavoriteRepository;
import com.xsh.trueused.repository.ProductRepository;
import com.xsh.trueused.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public void add(Long productId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (favoriteRepository.existsByUserAndProduct(user, product)) {
            return; // 幂等
        }
        Favorite f = new Favorite();
        f.setUser(user);
        f.setProduct(product);
        favoriteRepository.save(f);
        product.setFavoritesCount(favoriteRepository.countByProduct(product));
    }

    @Transactional
    public void remove(Long productId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        favoriteRepository.findByUserAndProduct(user, product).ifPresent(f -> {
            favoriteRepository.delete(f);
            product.setFavoritesCount(favoriteRepository.countByProduct(product));
        });
    }

    @Transactional(readOnly = true)
    public Page<FavoriteDTO> list(Long userId, int page, int size) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return favoriteRepository.findByUser(user, PageRequest.of(page, size))
                .map(FavoriteMapper::toDTO);
    }
}
