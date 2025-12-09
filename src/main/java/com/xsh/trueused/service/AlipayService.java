package com.xsh.trueused.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.xsh.trueused.config.AlipayConfiguration;
import com.xsh.trueused.dto.AlipayRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlipayService {

    private final AlipayClient alipayClient;
    private final AlipayConfiguration alipayConfiguration;

    public String createPayment(AlipayRequest alipayRequest) throws Exception {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayConfiguration.getNotifyUrl());
        request.setReturnUrl(alipayConfiguration.getReturnUrl());

        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(alipayRequest.getOutTradeNo());
        model.setTotalAmount(alipayRequest.getTotalAmount());
        model.setSubject(alipayRequest.getSubject());
        model.setBody(alipayRequest.getBody());
        model.setProductCode("FAST_INSTANT_TRADE_PAY");

        request.setBizModel(model);

        return alipayClient.pageExecute(request).getBody();
    }
}
