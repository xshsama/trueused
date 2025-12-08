package com.xsh.trueused.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.entity.Product;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.repository.CommentRepository;
import com.xsh.trueused.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeatCalculationService {

    private final ProductRepository productRepository;
    private final CommentRepository commentRepository;

    /**
     * Update heat score for all active products every hour.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void updateAllProductHeats() {
        log.info("Starting scheduled heat score update...");
        List<Product> products = productRepository.findByStatus(ProductStatus.AVAILABLE);

        for (Product product : products) {
            calculateAndSaveHeat(product);
        }
        log.info("Completed heat score update for {} products.", products.size());
    }

    @Transactional
    public void calculateAndSaveHeat(Product product) {
        double score = calculateHeat(product);
        product.setHeatScore(score);
        productRepository.save(product);
    }

    private double calculateHeat(Product product) {
        // 1. Base Score: Views
        long views = product.getViewsCount() != null ? product.getViewsCount() : 0;

        // 2. Interaction Score: Favorites * 10 + Comments * 5
        long favorites = product.getFavoritesCount() != null ? product.getFavoritesCount() : 0;
        long comments = commentRepository.countByProduct(product);

        double interactionScore = (favorites * 10) + (comments * 5);

        // 3. Value Score: (Original - Current) / Original
        double valueMultiplier = 1.0;
        if (product.getOriginalPrice() != null && product.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0
                && product.getPrice() != null) {
            BigDecimal original = product.getOriginalPrice();
            BigDecimal current = product.getPrice();

            if (original.compareTo(current) > 0) {
                BigDecimal discount = original.subtract(current).divide(original, 2, java.math.RoundingMode.HALF_UP);
                // E.g. 50% off -> 0.5. Multiplier = 1 + 0.5 = 1.5
                valueMultiplier = 1.0 + discount.doubleValue();
            }
        }

        // 4. Time Decay: (Hours since creation + 2) ^ 1.5
        long hoursSinceCreation = Duration.between(product.getCreatedAt(), java.time.Instant.now()).toHours();
        // Avoid negative hours if clock skew or just created
        if (hoursSinceCreation < 0)
            hoursSinceCreation = 0;

        double timeDecay = Math.pow(hoursSinceCreation + 2, 1.5);

        // Final Formula
        double rawScore = (views + interactionScore) * valueMultiplier;
        return rawScore / timeDecay;
    }
}
