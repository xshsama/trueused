package com.xsh.trueused.order.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xsh.trueused.order.dto.OrderDTO;
import com.xsh.trueused.product.dto.ProductDTO;
import com.xsh.trueused.entity.Order;
import com.xsh.trueused.address.mapper.AddressMapper;
import com.xsh.trueused.product.mapper.ProductMapper;
import com.xsh.trueused.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private static final Logger log = LoggerFactory.getLogger(OrderMapper.class);

    private final ObjectMapper objectMapper;

    public OrderDTO toDTO(Order order) {
        if (order == null)
            return null;
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setBuyer(UserMapper.toDTO(order.getBuyer()));
        dto.setSeller(UserMapper.toDTO(order.getSeller()));

        // 优先使用快照
        ProductDTO productDTO = null;
        if (order.getProductSnapshot() != null && !order.getProductSnapshot().isEmpty()) {
            try {
                productDTO = objectMapper.readValue(order.getProductSnapshot(), ProductDTO.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse product snapshot for order {}", order.getId(), e);
            }
        }

        // 如果快照不存在或解析失败，回退到关联查询
        if (productDTO == null) {
            productDTO = ProductMapper.toDTO(order.getProduct());
        }

        dto.setProduct(ProductMapper.enrich(productDTO));
        dto.setAddress(AddressMapper.toDTO(order.getAddress()));
        dto.setPrice(order.getPrice());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        // 物流信息映射
        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setExpressCompany(order.getExpressCompany());
        dto.setExpressCode(order.getExpressCode());
        dto.setShippedAt(order.getShippedAt());
        dto.setEstimatedDeliveryTime(order.getEstimatedDeliveryTime());
        dto.setDeliveredAt(order.getDeliveredAt());

        return dto;
    }
}
