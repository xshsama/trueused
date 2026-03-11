package com.xsh.trueused.order.payment;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.xsh.trueused.entity.Order;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.enums.ProductTradeModel;
import com.xsh.trueused.order.enums.OrderStatus;
import com.xsh.trueused.order.repository.OrderRepository;
import com.xsh.trueused.notification.service.NotificationService;
import com.xsh.trueused.product.service.ProductService;
import com.xsh.trueused.wallet.service.WalletService;
import com.xsh.trueused.order.state.OrderStateMachine;
import com.xsh.trueused.order.state.OrderTransition;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WalletOrderPaymentStrategy implements OrderPaymentStrategy {

    private final OrderStateMachine orderStateMachine;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final NotificationService notificationService;
    private final WalletService walletService;

    @Override
    public OrderPaymentType type() {
        return OrderPaymentType.WALLET;
    }

    @Override
    public OrderPaymentResult execute(Order order, OrderPaymentContext context) {
        orderStateMachine.assertCanTransit(order.getStatus(), OrderTransition.PAY, "Order cannot be paid");

        walletService.payOrder(context.buyerId(), order.getId(), order.getPrice(), context.password());

        order.setStatus(resolvePostPaymentStatus(order));
        order.setPaymentTime(Instant.now());
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
