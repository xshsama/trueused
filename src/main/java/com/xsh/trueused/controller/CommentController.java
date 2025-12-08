package com.xsh.trueused.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.dto.CommentDTO;
import com.xsh.trueused.dto.CreateCommentRequest;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.service.CommentService;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentDTO> createComment(
            @RequestBody @Validated CreateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        CommentDTO comment = commentService.createComment(request, currentUser.getId());
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<Page<CommentDTO>> getProductComments(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CommentDTO> comments = commentService.getProductComments(productId, pageable);
        return ResponseEntity.ok(comments);
    }
}
