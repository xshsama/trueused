package com.xsh.trueused.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.dto.CreateReviewRequest;
import com.xsh.trueused.dto.ReplyReviewRequest;
import com.xsh.trueused.dto.ReviewDTO;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.service.ReviewService;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(
            @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ReviewDTO review = reviewService.createReview(request, currentUser.getId());
        return ResponseEntity.ok(review);
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<Page<ReviewDTO>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReviewDTO> reviews = reviewService.getProductReviews(productId, pageable);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReviewDTO>> getMyReviews(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<ReviewDTO> reviews = reviewService.getMyReviews(currentUser.getId());
        return ResponseEntity.ok(reviews);
    }

    @PutMapping("/{id}/reply")
    public ResponseEntity<ReviewDTO> replyReview(
            @PathVariable Long id,
            @RequestBody ReplyReviewRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ReviewDTO review = reviewService.replyReview(id, request, currentUser.getId());
        return ResponseEntity.ok(review);
    }
}
