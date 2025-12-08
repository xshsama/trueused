package com.xsh.trueused.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.dto.BrowsingHistoryDTO;
import com.xsh.trueused.entity.BrowsingHistory;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.mapper.ProductMapper;
import com.xsh.trueused.repository.BrowsingHistoryRepository;
import com.xsh.trueused.repository.ProductRepository;
import com.xsh.trueused.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrowsingHistoryService {

    private final BrowsingHistoryRepository browsingHistoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public void recordHistory(Long userId, Long productId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        BrowsingHistory history = browsingHistoryRepository.findByUserIdAndProductId(userId, productId);
        if (history == null) {
            history = new BrowsingHistory();
            history.setUser(user);
            history.setProduct(product);
        }
        history.setViewedAt(Instant.now());
        browsingHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public Page<BrowsingHistoryDTO> getUserHistory(Long userId, Pageable pageable) {
        return browsingHistoryRepository.findByUserIdOrderByViewedAtDesc(userId, pageable)
                .map(history -> new BrowsingHistoryDTO(
                        history.getId(),
                        ProductMapper.toDTO(history.getProduct()),
                        history.getViewedAt()));
    }

    @Transactional
    public void clearHistory(Long userId) {
        // Implementation for clearing history if needed, or delete specific items
        // For now, maybe just delete all for user?
        // browsingHistoryRepository.deleteByUserId(userId); // Need to add to repo
    }
}
