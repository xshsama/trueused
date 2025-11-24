package com.xsh.trueused.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    @EntityGraph(attributePaths = { "buyer", "seller", "product", "address" })
    List<Order> findByBuyerId(Long buyerId);

    @EntityGraph(attributePaths = { "buyer", "seller", "product", "address" })
    List<Order> findBySellerId(Long sellerId);

    @Override
    @EntityGraph(attributePaths = { "buyer", "seller", "product", "address" })
    Optional<Order> findById(Long id);
}