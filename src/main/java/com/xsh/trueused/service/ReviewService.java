package com.xsh.trueused.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.xsh.trueused.dto.CreateReviewRequest;
import com.xsh.trueused.dto.ReplyReviewRequest;
import com.xsh.trueused.dto.ReviewDTO;
import com.xsh.trueused.entity.Order;
import com.xsh.trueused.entity.Review;
import com.xsh.trueused.enums.OrderStatus;
import com.xsh.trueused.mapper.ReviewMapper;
import com.xsh.trueused.repository.OrderRepository;
import com.xsh.trueused.repository.ReviewRepository;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Transactional
    public ReviewDTO createReview(CreateReviewRequest request, Long buyerId) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only review your own orders");
        }

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must be completed to review");
        }

        if (reviewRepository.findByOrderId(order.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review already exists for this order");
        }

        Review review = new Review();
        review.setOrder(order);
        review.setProduct(order.getProduct());
        review.setBuyer(order.getBuyer());
        review.setSeller(order.getSeller());
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setIsAnonymous(request.getIsAnonymous());

        Review savedReview = reviewRepository.save(review);
        return ReviewMapper.INSTANCE.toDTO(savedReview);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDTO> getProductReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable)
                .map(ReviewMapper.INSTANCE::toDTO);
    }

    @Transactional(readOnly = true)
    public List<ReviewDTO> getMyReviews(Long buyerId) {
        return reviewRepository.findByBuyerId(buyerId).stream()
                .map(ReviewMapper.INSTANCE::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewDTO replyReview(Long reviewId, ReplyReviewRequest request, Long sellerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));

        if (!review.getSeller().getId().equals(sellerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only reply to reviews of your products");
        }

        review.setSellerReply(request.getReplyContent());
        review.setSellerReplyAt(Instant.now());

        Review savedReview = reviewRepository.save(review);
        return ReviewMapper.INSTANCE.toDTO(savedReview);
    }
}
