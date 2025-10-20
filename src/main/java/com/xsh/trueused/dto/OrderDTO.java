package com.xsh.trueused.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.xsh.trueused.enums.OrderStatus;

import lombok.Data;

@Data
public class OrderDTO {
    private Long id;
    private UserDTO buyer;
    private UserDTO seller;
    private ProductDTO product;
    private BigDecimal price;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}