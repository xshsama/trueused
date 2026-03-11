package com.xsh.trueused.order.payment;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.xsh.trueused.entity.Order;
import com.xsh.trueused.order.enums.OrderStatus;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.enums.ProductTradeModel;
import com.xsh.trueused.order.repository.OrderRepository;
import com.xsh.trueused.notification.service.NotificationService;
import com.xsh.trueused.product.service.ProductService;
import com.xsh.trueused.order.state.OrderStateMachine;
import com.xsh.trueused.order.state.OrderTransition;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CallbackOrderPaymentStrategy implements OrderPaymentStrategy {

    private static final Logger log = LoggerFactory.getLogger(CallbackOrderPaymentStrategy.class);

    private final OrderStateMachine orderStateMachine;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final NotificationService notificationService;

    @Override
    public OrderPaymentType type() {
        return OrderPaymentType.CALLBACK;
    }

    @Override
    public OrderPaymentResult execute(Order order, OrderPaymentContext context) {
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.PENDING_SHIPMENT
                || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.COMPLETED) {
            log.info("Order {} is already paid, skipping update.", order.getId());
            return OrderPaymentResult.SKIPPED;
        }

        if (!orderStateMachine.canTransit(order.getStatus(), OrderTransition.PAY)) {
            log.warn("Order {} status is {}, cannot be paid.", order.getId(), order.getStatus());
            return OrderPaymentResult.SKIPPED;
        }

        order.setStatus(resolvePostPaymentStatus(order));
        order.setPaymentTime(Instant.now());
        order.setTransactionId(context.transactionId());
        orderRepository.save(order);

        productService.updateProductStatus(order.getProduct().getId(), ProductStatus.SOLD);
        notificationService.createNotification(
                order.getSeller().getId(),
                "订单已付款",
                buildPaidNotice(order),
                "ORDER_PAID",
                order.getId());
        return OrderPaymentResult.PROCESSED;
    }

    private OrderStatus resolvePostPaymentStatus(Order order) {
        return order.getProduct() != null && order.getProduct().getTradeModel() == ProductTradeModel.OFFICIAL_INSPECTION
                ? OrderStatus.PENDING_SHIPMENT
                : orderStateMachine.nextStatus(order.getStatus(), OrderTransition.PAY);
    }

    private String buildPaidNotice(Order order) {
        if (order.getProduct() != null && order.getProduct().getTradeModel() == ProductTradeModel.OFFICIAL_INSPECTION) {
            return "订单 [" + order.getId() + "] 买家已付款，平台仓将安排出库。";
        }
        return "订单 [" + order.getId() + "] 买家已付款，请尽快发货。";
    }
}
