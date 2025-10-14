package com.xsh.trueused.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.xsh.trueused.entity.Favorite;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.entity.User;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserAndProduct(User user, Product product);

    Page<Favorite> findByUser(User user, Pageable pageable);

    boolean existsByUserAndProduct(User user, Product product);

    long countByProduct(Product product);
}
