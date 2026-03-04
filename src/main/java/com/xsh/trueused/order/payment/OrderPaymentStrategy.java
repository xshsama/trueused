package com.xsh.trueused.order.payment;

import com.xsh.trueused.entity.Order;

public interface OrderPaymentStrategy {
    OrderPaymentType type();

    OrderPaymentResult execute(Order order, OrderPaymentContext context);
}
