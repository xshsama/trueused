package com.xsh.trueused.observability.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class BusinessMetricsRecorderTest {

    @Test
    void shouldRecordSuccessfulPaymentAmount() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        BusinessMetricsRecorder recorder = new BusinessMetricsRecorder(meterRegistry);

        recorder.recordPayment("wallet", "processed", new BigDecimal("128.50"));

        double paymentCount = meterRegistry.get("trueused.order.payment")
                .tag("channel", "wallet")
                .tag("result", "processed")
                .counter()
                .count();
        double amountSum = meterRegistry.get("trueused.order.payment.amount")
                .tag("channel", "wallet")
                .summary()
                .totalAmount();

        assertEquals(1.0d, paymentCount);
        assertEquals(128.50d, amountSum);
    }

    @Test
    void shouldRecordCommandLatency() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        BusinessMetricsRecorder recorder = new BusinessMetricsRecorder(meterRegistry);

        Timer.Sample sample = recorder.startCommandSample();
        recorder.stopCommandSample("create_order", "success", sample);

        long count = meterRegistry.get("trueused.order.command.duration")
                .tag("operation", "create_order")
                .tag("result", "success")
                .timer()
                .count();

        assertEquals(1L, count);
    }

    @Test
    void shouldRecordOrderTransition() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        BusinessMetricsRecorder recorder = new BusinessMetricsRecorder(meterRegistry);

        recorder.recordOrderTransition("cancel_order", "pending_payment", "cancelled", "success", "none");

        double count = meterRegistry.get("trueused.order.transition")
                .tag("operation", "cancel_order")
                .tag("from_status", "pending_payment")
                .tag("to_status", "cancelled")
                .tag("result", "success")
                .tag("reason", "none")
                .counter()
                .count();

        assertEquals(1.0d, count);
    }

    @Test
    void shouldRecordScheduledJobAffectedCount() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        BusinessMetricsRecorder recorder = new BusinessMetricsRecorder(meterRegistry);

        recorder.recordScheduledJob("cancel_expired_orders", "success", 3);

        double count = meterRegistry.get("trueused.order.scheduled.job")
                .tag("job", "cancel_expired_orders")
                .tag("result", "success")
                .counter()
                .count();
        double affected = meterRegistry.get("trueused.order.scheduled.job.affected")
                .tag("job", "cancel_expired_orders")
                .tag("result", "success")
                .summary()
                .totalAmount();

        assertEquals(1.0d, count);
        assertEquals(3.0d, affected);
    }
}
