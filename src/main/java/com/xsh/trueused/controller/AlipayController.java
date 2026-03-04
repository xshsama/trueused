package com.xsh.trueused.controller;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alipay.api.internal.util.AlipaySignature;
import com.xsh.trueused.config.AlipayConfiguration;
import com.xsh.trueused.dto.AlipayRequest;
import com.xsh.trueused.service.AlipayService;
import com.xsh.trueused.service.OrderService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/alipay")
@RequiredArgsConstructor
@Slf4j
public class AlipayController {

    private final AlipayService alipayService;
    private final AlipayConfiguration alipayConfiguration;
    private final OrderService orderService;

    @PostMapping("/pay")
    public ResponseEntity<String> pay(@RequestBody AlipayRequest alipayRequest) {
        try {
            String form = alipayService.createPayment(alipayRequest);
            return ResponseEntity.ok(form);
        } catch (Exception e) {
            log.error("Payment creation failed for outTradeNo {}", alipayRequest.getOutTradeNo(), e);
            return ResponseEntity.internalServerError().body("Payment creation failed");
        }
    }

    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        try {
            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext();) {
                String name = iter.next();
                String[] values = requestParams.get(name);
                StringBuilder valueStr = new StringBuilder();
                for (int i = 0; i < values.length; i++) {
                    valueStr.append(i == values.length - 1 ? values[i] : values[i] + ",");
                }
                params.put(name, valueStr.toString());
            }

            boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayConfiguration.getPublicKey(), "UTF-8",
                    "RSA2");

            if (signVerified) {
                String outTradeNo = params.get("out_trade_no");
                String tradeStatus = params.get("trade_status");
                String tradeNo = params.get("trade_no"); // Alipay transaction ID

                log.info("Alipay notify verified. outTradeNo={}, tradeStatus={}", outTradeNo, tradeStatus);

                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    try {
                        Long orderId = Long.parseLong(outTradeNo);
                        orderService.handlePaymentSuccess(orderId, tradeNo);
                        log.info("Order paid and updated: {}", outTradeNo);
                        return "success";
                    } catch (NumberFormatException e) {
                        log.warn("Invalid order ID format: {}", outTradeNo);
                        return "failure";
                    } catch (Exception e) {
                        log.error("Failed to update order status for outTradeNo={}", outTradeNo, e);
                        // Return failure to trigger Alipay retry mechanism
                        return "failure";
                    }
                }
                return "success";
            } else {
                log.warn("Alipay notify signature verification failed.");
                return "failure";
            }
        } catch (Exception e) {
            log.error("Alipay notify handling failed", e);
            return "failure";
        }
    }
}
