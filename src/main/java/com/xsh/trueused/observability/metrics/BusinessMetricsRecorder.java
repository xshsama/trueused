package com.xsh.trueused.observability.metrics;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BusinessMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public Timer.Sample startCommandSample() {
        return Timer.start(meterRegistry);
    }

    public void stopCommandSample(String operation, String result, Timer.Sample sample) {
        sample.stop(Timer.builder("trueused.order.command.duration")
                .description("Latency of order-domain write commands.")
                .tag("operation", operation)
                .tag("result", result)
                .register(meterRegistry));
    }

    public void recordOrderCreated(String tradeModel, String result, String reason, BigDecimal amount) {
        Counter.builder("trueused.order.created")
                .description("Order creation attempts.")
                .tag("trade_model", tradeModel)
                .tag("result", result)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();

        if (amount == null || amount.signum() < 0) {
            return;
        }

        DistributionSummary.builder("trueused.order.created.amount")
                .description("Order amount captured at creation time.")
                .baseUnit("cny")
                .tag("trade_model", tradeModel)
                .register(meterRegistry)
                .record(amount.doubleValue());
    }

    public void recordPayment(String channel, String result, BigDecimal amount) {
        Counter.builder("trueused.order.payment")
                .description("Order payment processing results.")
                .tag("channel", channel)
                .tag("result", result)
                .register(meterRegistry)
                .increment();

        if (!"processed".equals(result) || amount == null || amount.signum() < 0) {
            return;
        }

        DistributionSummary.builder("trueused.order.payment.amount")
                .description("Paid GMV amount recorded from successful order payments.")
                .baseUnit("cny")
                .tag("channel", channel)
                .register(meterRegistry)
                .record(amount.doubleValue());
    }

    public void recordPaymentCallback(String result) {
        Counter.builder("trueused.payment.callback")
                .description("Alipay callback processing results.")
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    public void recordShipment(String fulfillment, String result) {
        Counter.builder("trueused.order.shipment")
                .description("Shipment command execution results.")
                .tag("fulfillment", fulfillment)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    public void recordOrderTransition(String operation, String fromStatus, String toStatus, String result, String reason) {
        Counter.builder("trueused.order.transition")
                .description("Order lifecycle transition results.")
                .tag("operation", operation)
                .tag("from_status", fromStatus)
                .tag("to_status", toStatus)
                .tag("result", result)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public void recordScheduledJob(String job, String result, int affectedCount) {
        Counter.builder("trueused.order.scheduled.job")
                .description("Scheduled order maintenance job executions.")
                .tag("job", job)
                .tag("result", result)
                .register(meterRegistry)
                .increment();

        DistributionSummary.builder("trueused.order.scheduled.job.affected")
                .description("Number of orders affected by each scheduled job execution.")
                .baseUnit("orders")
                .tag("job", job)
                .tag("result", result)
                .register(meterRegistry)
                .record(Math.max(affectedCount, 0));
    }

    public String normalize(Enum<?> value) {
        if (value == null) {
            return "unknown";
        }
        return value.name().toLowerCase(Locale.ROOT);
    }
}
