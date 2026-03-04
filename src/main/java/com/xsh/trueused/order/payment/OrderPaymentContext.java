package com.xsh.trueused.order.payment;

public record OrderPaymentContext(
        Long buyerId,
        String password,
        String transactionId) {

    public static OrderPaymentContext direct(Long buyerId) {
        return new OrderPaymentContext(buyerId, null, null);
    }

    public static OrderPaymentContext wallet(Long buyerId, String password) {
        return new OrderPaymentContext(buyerId, password, null);
    }

    public static OrderPaymentContext callback(String transactionId) {
        return new OrderPaymentContext(null, null, transactionId);
    }
}
