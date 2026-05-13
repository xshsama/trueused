package com.xsh.trueused.order.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.xsh.trueused.order.enums.OrderStatus;

class OrderStateMachineTest {

    private OrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine();
    }

    @Test
    void shouldAllowDirectTradingOrderLifecycle() {
        assertEquals(OrderStatus.PAID,
                stateMachine.nextStatus(OrderStatus.PENDING_PAYMENT, OrderTransition.PAY));
        assertEquals(OrderStatus.SHIPPED,
                stateMachine.nextStatus(OrderStatus.PAID, OrderTransition.SHIP));
        assertEquals(OrderStatus.COMPLETED,
                stateMachine.nextStatus(OrderStatus.SHIPPED, OrderTransition.CONFIRM_DELIVERY));
    }

    @Test
    void shouldAllowPlatformInspectionShipmentFromPendingShipment() {
        assertEquals(OrderStatus.SHIPPED,
                stateMachine.nextStatus(OrderStatus.PENDING_SHIPMENT, OrderTransition.SHIP));
    }

    @Test
    void shouldRejectShippingBeforePayment() {
        assertFalse(stateMachine.canTransit(OrderStatus.PENDING_PAYMENT, OrderTransition.SHIP));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateMachine.nextStatus(OrderStatus.PENDING_PAYMENT, OrderTransition.SHIP));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void shouldRejectConfirmingDeliveryBeforeShipment() {
        assertFalse(stateMachine.canTransit(OrderStatus.PAID, OrderTransition.CONFIRM_DELIVERY));
        assertTrue(stateMachine.canTransit(OrderStatus.SHIPPED, OrderTransition.CONFIRM_DELIVERY));
    }
}
