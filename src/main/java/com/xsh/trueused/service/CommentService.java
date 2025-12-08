package com.xsh.trueused.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.xsh.trueused.dto.CommentDTO;
import com.xsh.trueused.dto.CreateCommentRequest;
import com.xsh.trueused.dto.PublicUserDTO;
import com.xsh.trueused.entity.Comment;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.repository.CommentRepository;
import com.xsh.trueused.repository.ProductRepository;
import com.xsh.trueused.repository.UserRepository;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;

    @Transactional
    public CommentDTO createComment(CreateCommentRequest request, Long userId) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Comment comment = new Comment();
        comment.setProduct(product);
        comment.setUser(user);
        comment.setContent(request.content());

        if (request.parentId() != null) {
            Comment parent = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent comment not found"));
            comment.setParent(parent);
        }

        Comment saved = commentRepository.save(comment);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<CommentDTO> getProductComments(Long productId, Pageable pageable) {
        return commentRepository.findByProductId(productId, pageable)
                .map(this::toDTO);
    }

    private CommentDTO toDTO(Comment comment) {
        PublicUserDTO userDTO = new PublicUserDTO(
                comment.getUser().getId(),
                comment.getUser().getUsername(),
                comment.getUser().getNickname(),
                comment.getUser().getAvatarUrl(),
                comment.getUser().getBio(),
                comment.getUser().getCreatedAt(),
                0, 0 // Stats not needed here
        );

        List<CommentDTO> replies = comment.getReplies().stream()
                .filter(r -> !r.getIsDeleted())
                .map(this::toDTO)
                .collect(Collectors.toList());

        return new CommentDTO(
                comment.getId(),
                comment.getProduct().getId(),
                userDTO,
                comment.getContent(),
                comment.getCreatedAt(),
                replies);
    }
}
