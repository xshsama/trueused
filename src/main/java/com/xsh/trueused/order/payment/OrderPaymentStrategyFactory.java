package com.xsh.trueused.order.payment;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class OrderPaymentStrategyFactory {

    private final Map<OrderPaymentType, OrderPaymentStrategy> strategyMap;

    public OrderPaymentStrategyFactory(List<OrderPaymentStrategy> strategies) {
        this.strategyMap = new EnumMap<>(OrderPaymentType.class);
        for (OrderPaymentStrategy strategy : strategies) {
            strategyMap.put(strategy.type(), strategy);
        }
    }

    public OrderPaymentStrategy get(OrderPaymentType type) {
        OrderPaymentStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalStateException("No payment strategy configured for type: " + type);
        }
        return strategy;
    }
}
