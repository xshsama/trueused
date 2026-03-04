package com.xsh.trueused.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.xsh.trueused.address.dto.AddressDTO;
import com.xsh.trueused.product.dto.ProductDTO;
import com.xsh.trueused.order.dto.ShippingInfoDTO;
import com.xsh.trueused.user.dto.UserDTO;
import com.xsh.trueused.order.enums.OrderStatus;

import lombok.Data;

@Data
public class OrderDTO {
    private Long id;
    private UserDTO buyer;
    private UserDTO seller;
    private ProductDTO product;
    private AddressDTO address;
    private BigDecimal price;
    private OrderStatus status;
    private Instant paymentTime;
    private Instant createdAt;
    private Instant updatedAt;

    // 物流信息
    private String trackingNumber;
    private String expressCompany;
    private String expressCode;
    private Instant shippedAt;
    private Instant estimatedDeliveryTime;
    private Instant deliveredAt;

    // 详细物流追踪信息（可选，按需加载）
    private ShippingInfoDTO shippingInfo;
}
