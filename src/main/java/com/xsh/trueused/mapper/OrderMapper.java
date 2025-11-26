package com.xsh.trueused.mapper;

import com.xsh.trueused.dto.OrderDTO;
import com.xsh.trueused.entity.Order;

public final class OrderMapper {
    private OrderMapper() {
    }

    public static final OrderMapper INSTANCE = new OrderMapper();

    public OrderDTO toDTO(Order order) {
        if (order == null)
            return null;
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setBuyer(UserMapper.toDTO(order.getBuyer()));
        dto.setSeller(UserMapper.toDTO(order.getSeller()));
        dto.setProduct(ProductMapper.toDTO(order.getProduct()));
        // map address using AddressMapper if available
        dto.setAddress(AddressMapper.INSTANCE == null ? null : AddressMapper.INSTANCE.toDTO(order.getAddress()));
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