package com.xsh.trueused.review.service;

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

import com.xsh.trueused.review.dto.CreateReviewRequest;
import com.xsh.trueused.review.dto.ReplyReviewRequest;
import com.xsh.trueused.review.dto.ReviewDTO;
import com.xsh.trueused.entity.Order;
import com.xsh.trueused.entity.Review;
import com.xsh.trueused.entity.ReviewImage;
import com.xsh.trueused.order.enums.OrderStatus;
import com.xsh.trueused.review.mapper.ReviewMapper;
import com.xsh.trueused.order.repository.OrderRepository;
import com.xsh.trueused.review.repository.ReviewRepository;

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

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (String url : request.getImages()) {
                ReviewImage image = new ReviewImage();
                image.setReview(review);
                image.setUrl(url);
                review.getImages().add(image);
            }
        }

        Review savedReview = reviewRepository.save(review);
        return ReviewMapper.toDTO(savedReview);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDTO> getProductReviews(Long productId, Pageable pageable) {
        return (Page<ReviewDTO>) reviewRepository.findByProductId(productId, pageable)
                .map(ReviewMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDTO> getSellerReviews(Long sellerId, Pageable pageable) {
        return (Page<ReviewDTO>) reviewRepository.findBySellerId(sellerId, pageable)
                .map(ReviewMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public List<ReviewDTO> getMyReviews(Long buyerId) {
        return reviewRepository.findByBuyerId(buyerId).stream()
                .map(ReviewMapper::toDTO)
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
        return ReviewMapper.toDTO(savedReview);
    }
}
