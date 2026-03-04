package com.xsh.trueused.interaction.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.xsh.trueused.entity.Comment;
import com.xsh.trueused.entity.Product;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // Only fetch top-level comments (parent is null)
    @Query("SELECT c FROM Comment c WHERE c.product.id = :productId AND c.parent IS NULL AND c.isDeleted = false")
    Page<Comment> findByProductId(Long productId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.targetUser.id = :targetUserId AND c.parent IS NULL AND c.isDeleted = false")
    Page<Comment> findByTargetUserId(Long targetUserId, Pageable pageable);

    long countByProduct(Product product);
}
