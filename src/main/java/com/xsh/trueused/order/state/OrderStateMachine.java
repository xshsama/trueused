package com.xsh.trueused.order.state;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.xsh.trueused.order.enums.OrderStatus;

@Component
public class OrderStateMachine {

    private final Map<OrderTransition, Map<OrderStatus, OrderStatus>> transitionMap;

    public OrderStateMachine() {
        this.transitionMap = new EnumMap<>(OrderTransition.class);
        register(OrderTransition.PAY, OrderStatus.PENDING_PAYMENT, OrderStatus.PAID);
        register(OrderTransition.SHIP, OrderStatus.PAID, OrderStatus.SHIPPED);
        register(OrderTransition.SHIP, OrderStatus.PENDING_SHIPMENT, OrderStatus.SHIPPED);
        register(OrderTransition.CONFIRM_DELIVERY, OrderStatus.SHIPPED, OrderStatus.COMPLETED);
        register(OrderTransition.CANCEL, OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED);
        register(OrderTransition.CANCEL, OrderStatus.PAID, OrderStatus.CANCELLED);
        register(OrderTransition.CANCEL, OrderStatus.PENDING_SHIPMENT, OrderStatus.CANCELLED);
        register(OrderTransition.REFUND, OrderStatus.PAID, OrderStatus.REFUNDED);
        register(OrderTransition.REFUND, OrderStatus.PENDING_SHIPMENT, OrderStatus.REFUNDED);
        register(OrderTransition.REFUND, OrderStatus.SHIPPED, OrderStatus.REFUNDED);
        register(OrderTransition.EXPIRE, OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED);
    }

    private void register(OrderTransition transition, OrderStatus from, OrderStatus to) {
        transitionMap.computeIfAbsent(transition, key -> new EnumMap<>(OrderStatus.class)).put(from, to);
    }

    public boolean canTransit(OrderStatus current, OrderTransition transition) {
        Map<OrderStatus, OrderStatus> transitions = transitionMap.get(transition);
        return transitions != null && transitions.containsKey(current);
    }

    public OrderStatus nextStatus(OrderStatus current, OrderTransition transition) {
        Map<OrderStatus, OrderStatus> transitions = transitionMap.get(transition);
        if (transitions == null || !transitions.containsKey(current)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot " + transition.action());
        }
        return transitions.get(current);
    }

    public void assertCanTransit(OrderStatus current, OrderTransition transition, String conflictMessage) {
        if (!canTransit(current, transition)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, conflictMessage);
        }
    }
}
