package com.xsh.trueused.order.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.Order;
import com.xsh.trueused.order.enums.OrderStatus;
import com.xsh.trueused.enums.ProductStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    @EntityGraph(attributePaths = { "buyer", "seller", "product", "address" })
    List<Order> findByBuyerId(Long buyerId);

    @EntityGraph(attributePaths = { "buyer", "seller", "product", "address" })
    List<Order> findBySellerId(Long sellerId);

    @Override
    @EntityGraph(attributePaths = { "buyer", "seller", "product", "address" })
    Optional<Order> findById(Long id);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant dateTime);

    List<Order> findByStatusAndPaymentTimeBefore(OrderStatus status, Instant paymentTime);

    List<Order> findByStatusAndShippedAtBefore(OrderStatus status, Instant shippedAt);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(o.price) FROM Order o WHERE o.seller.id = :sellerId AND o.status = :status")
    java.math.BigDecimal sumTotalAmountBySellerIdAndStatus(Long sellerId, OrderStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(o.price) FROM Order o WHERE o.seller.id = :sellerId AND o.status IN :statuses AND o.createdAt BETWEEN :start AND :end")
    java.math.BigDecimal sumPriceBySellerAndStatusInAndCreatedAtBetween(Long sellerId, java.util.Collection<OrderStatus> statuses, Instant start, Instant end);

    @org.springframework.data.jpa.repository.Query("""
            SELECT SUM(o.price)
            FROM Order o
            WHERE o.seller.id = :sellerId
              AND o.status IN :statuses
              AND o.product.status IN :productStatuses
              AND o.createdAt BETWEEN :start AND :end
            """)
    java.math.BigDecimal sumPriceBySellerAndStatusInAndProductStatusInAndCreatedAtBetween(
            Long sellerId,
            java.util.Collection<OrderStatus> statuses,
            java.util.Collection<ProductStatus> productStatuses,
            Instant start,
            Instant end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(o) FROM Order o WHERE o.seller.id = :sellerId AND o.status IN :statuses AND o.createdAt BETWEEN :start AND :end")
    Long countBySellerAndStatusInAndCreatedAtBetween(Long sellerId, java.util.Collection<OrderStatus> statuses, Instant start, Instant end);

    @org.springframework.data.jpa.repository.Query("""
            SELECT COUNT(o)
            FROM Order o
            WHERE o.seller.id = :sellerId
              AND o.status IN :statuses
              AND o.product.status IN :productStatuses
              AND o.createdAt BETWEEN :start AND :end
            """)
    Long countBySellerAndStatusInAndProductStatusInAndCreatedAtBetween(
            Long sellerId,
            java.util.Collection<OrderStatus> statuses,
            java.util.Collection<ProductStatus> productStatuses,
            Instant start,
            Instant end);

    List<Order> findBySellerIdAndStatusInAndCreatedAtBetween(Long sellerId, java.util.Collection<OrderStatus> statuses, Instant start, Instant end);

    @EntityGraph(attributePaths = { "buyer", "seller", "product", "address" })
    @org.springframework.data.jpa.repository.Query("""
            SELECT o
            FROM Order o
            WHERE o.seller.id = :sellerId
              AND o.status IN :statuses
              AND o.product.status IN :productStatuses
              AND o.createdAt BETWEEN :start AND :end
            """)
    List<Order> findBySellerIdAndStatusInAndProductStatusInAndCreatedAtBetween(
            Long sellerId,
            java.util.Collection<OrderStatus> statuses,
            java.util.Collection<ProductStatus> productStatuses,
            Instant start,
            Instant end);
}
