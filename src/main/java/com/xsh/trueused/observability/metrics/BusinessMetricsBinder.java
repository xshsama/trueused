package com.xsh.trueused.observability.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.xsh.trueused.entity.Order;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.order.enums.OrderStatus;
import com.xsh.trueused.order.repository.OrderRepository;
import com.xsh.trueused.product.repository.ProductRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BusinessMetricsBinder implements MeterBinder {

    private static final List<OrderStatus> TRACKED_ORDER_STATUSES = List.of(
            OrderStatus.PENDING_PAYMENT,
            OrderStatus.PAID,
            OrderStatus.PENDING_SHIPMENT,
            OrderStatus.SHIPPED,
            OrderStatus.COMPLETED,
            OrderStatus.REFUNDING,
            OrderStatus.REFUNDED,
            OrderStatus.CANCELLED);

    private static final List<ProductStatus> TRACKED_PRODUCT_STATUSES = List.of(
            ProductStatus.PENDING,
            ProductStatus.ON_SALE,
            ProductStatus.LOCKED,
            ProductStatus.SOLD,
            ProductStatus.OFF_SHELF);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Override
    public void bindTo(MeterRegistry registry) {
        TRACKED_ORDER_STATUSES.forEach(status -> Gauge.builder("trueused.orders.total",
                        () -> orderRepository.countByStatus(status))
                .description("Current order count grouped by lifecycle status.")
                .tag("status", normalize(status))
                .register(registry));

        TRACKED_PRODUCT_STATUSES.forEach(status -> Gauge.builder("trueused.products.total",
                        () -> productRepository.countByStatus(status))
                .description("Current product count grouped by sale status.")
                .tag("status", normalize(status))
                .register(registry));

        Gauge.builder("trueused.orders.pending.payment.oldest.age",
                        () -> oldestAgeSeconds(OrderStatus.PENDING_PAYMENT))
                .description("Age in seconds of the oldest pending-payment order.")
                .baseUnit("seconds")
                .register(registry);

        Gauge.builder("trueused.orders.pending.shipment.oldest.age",
                        () -> oldestAgeSeconds(OrderStatus.PENDING_SHIPMENT))
                .description("Age in seconds of the oldest pending-shipment order.")
                .baseUnit("seconds")
                .register(registry);
    }

    private double oldestAgeSeconds(OrderStatus status) {
        return orderRepository.findFirstByStatusOrderByCreatedAtAsc(status)
                .map(Order::getCreatedAt)
                .map(createdAt -> Duration.between(createdAt, Instant.now()).toSeconds())
                .orElse(0L);
    }

    private String normalize(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
