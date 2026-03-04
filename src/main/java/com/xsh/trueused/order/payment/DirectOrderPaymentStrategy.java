package com.xsh.trueused.order.payment;

import org.springframework.stereotype.Component;

import com.xsh.trueused.entity.Order;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.order.repository.OrderRepository;
import com.xsh.trueused.notification.service.NotificationService;
import com.xsh.trueused.product.service.ProductService;
import com.xsh.trueused.order.state.OrderStateMachine;
import com.xsh.trueused.order.state.OrderTransition;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DirectOrderPaymentStrategy implements OrderPaymentStrategy {

    private final OrderStateMachine orderStateMachine;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final NotificationService notificationService;

    @Override
    public OrderPaymentType type() {
        return OrderPaymentType.DIRECT;
    }

    @Override
    public OrderPaymentResult execute(Order order, OrderPaymentContext context) {
        orderStateMachine.assertCanTransit(order.getStatus(), OrderTransition.PAY, "Order cannot be paid");
        order.setStatus(orderStateMachine.nextStatus(order.getStatus(), OrderTransition.PAY));
        orderRepository.save(order);

        productService.updateProductStatus(order.getProduct().getId(), ProductStatus.SOLD);
        notificationService.createNotification(
                order.getSeller().getId(),
                "订单已付款",
                "订单 [" + order.getId() + "] 买家已付款，请尽快发货。",
                "ORDER_PAID",
                order.getId());
        return OrderPaymentResult.PROCESSED;
    }
}
