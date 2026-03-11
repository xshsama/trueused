package com.xsh.trueused.order.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xsh.trueused.order.dto.OrderDTO;
import com.xsh.trueused.order.dto.ShippingInfoDTO;
import com.xsh.trueused.entity.Order;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.enums.ProductTradeModel;
import com.xsh.trueused.order.mapper.OrderMapper;
import com.xsh.trueused.order.repository.OrderRepository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

@Service
public class OrderQueryService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ShippingService shippingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByBuyer(Long buyerId) {
        return orderRepository.findByBuyerId(buyerId).stream()
                .map(orderMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersBySeller(Long sellerId, String productName, String orderId, String buyerName,
            Pageable pageable) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("seller").get("id"), sellerId));

            if (productName != null && !productName.isEmpty()) {
                Join<Order, Product> productJoin = root.join("product");
                predicates.add(cb.like(productJoin.get("title"), "%" + productName + "%"));
            }

            if (orderId != null && !orderId.isEmpty()) {
                predicates.add(cb.equal(root.get("id"), orderId));
            }

            if (buyerName != null && !buyerName.isEmpty()) {
                Join<Order, User> buyerJoin = root.join("buyer");
                predicates.add(cb.like(buyerJoin.get("username"), "%" + buyerName + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return (Page<OrderDTO>) orders.map((Order order) -> orderMapper.toDTO(order));
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return orderMapper.toDTO(order);
    }

    @Transactional(readOnly = true)
    public ShippingInfoDTO getOrderShippingInfo(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getTrackingNumber() == null) {
            return null;
        }

        if (order.getShippingSnapshot() != null && !order.getShippingSnapshot().isBlank()) {
            try {
                ShippingInfoDTO snapshot = objectMapper.readValue(order.getShippingSnapshot(), ShippingInfoDTO.class);
                return shippingService.refreshShippingInfo(snapshot);
            } catch (Exception e) {
                // fall through to legacy reconstruction when snapshot is malformed or from older data
            }
        }

        ShippingInfoDTO shippingInfo = shippingService.getShippingInfo(order.getTrackingNumber());

        // 如果缓存中没有物流信息（可能是服务重启导致），尝试重建
        if (shippingInfo == null && order.getShippedAt() != null) {
            String senderCity = "发货地";
            String senderDistrict = "";

            shippingInfo = shippingService.reconstructShippingInfo(
                    order.getTrackingNumber(),
                    order.getExpressCompany(),
                    order.getShippedAt(),
                    order.getProduct() != null && order.getProduct().getTradeModel() == ProductTradeModel.OFFICIAL_INSPECTION
                            ? "PLATFORM_OUTBOUND"
                            : "SELLER_OUTBOUND",
                    order.getProduct() != null && order.getProduct().getTradeModel() == ProductTradeModel.OFFICIAL_INSPECTION
                            ? "平台仓"
                            : senderCity,
                    order.getProduct() != null && order.getProduct().getTradeModel() == ProductTradeModel.OFFICIAL_INSPECTION
                            ? "质检仓"
                            : senderDistrict,
                    order.getAddress());
        }

        return shippingInfo;
    }
}
