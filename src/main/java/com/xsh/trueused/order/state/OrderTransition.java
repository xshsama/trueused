package com.xsh.trueused.order.state;

public enum OrderTransition {
    PAY("pay"),
    SHIP("ship"),
    CONFIRM_DELIVERY("confirm delivery"),
    CANCEL("cancel"),
    REFUND("refund"),
    EXPIRE("expire");

    private final String action;

    OrderTransition(String action) {
        this.action = action;
    }

    public String action() {
        return action;
    }
}
