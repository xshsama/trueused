package com.xsh.trueused.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.dto.CreateOrderRequest;
import com.xsh.trueused.dto.OrderDTO;
import com.xsh.trueused.entity.Order;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.enums.OrderStatus;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.mapper.OrderMapper;
import com.xsh.trueused.repository.OrderRepository;
import com.xsh.trueused.repository.ProductRepository;
import com.xsh.trueused.repository.UserRepository;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest createOrderRequest, Long buyerId) {
        // 1. 查找商品
        Product product = productRepository.findById(createOrderRequest.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // 2. 检查商品状态是否可购买
        if (product.getStatus() != ProductStatus.AVAILABLE) {
            throw new RuntimeException("Product is not available for purchase");
        }

        // 3. 查找买家
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));

        // 4. 不能购买自己的商品
        if (product.getSeller().getId().equals(buyerId)) {
            throw new RuntimeException("You cannot buy your own product");
        }

        // 5. 创建并保存订单
        Order order = new Order();
        order.setBuyer(buyer);
        order.setSeller(product.getSeller());
        order.setProduct(product);
        order.setPrice(product.getPrice());
        order.setStatus(OrderStatus.PENDING); // 初始状态为待处理

        Order savedOrder = orderRepository.save(order);

        // 6. 更新商品状态为已售出
        product.setStatus(ProductStatus.SOLD);
        productRepository.save(product);

        // 7. 转换并返回 DTO
        return OrderMapper.INSTANCE.toDTO(savedOrder);
    }

    public List<OrderDTO> getOrdersByBuyer(Long buyerId) {
        return orderRepository.findByBuyerId(buyerId).stream()
                .map(OrderMapper.INSTANCE::toDTO)
                .collect(Collectors.toList());
    }

    public List<OrderDTO> getOrdersBySeller(Long sellerId) {
        return orderRepository.findBySellerId(sellerId).stream()
                .map(OrderMapper.INSTANCE::toDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return OrderMapper.INSTANCE.toDTO(order);
    }

    @Transactional
    public OrderDTO payOrder(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new RuntimeException("You are not authorized to pay for this order");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order cannot be paid");
        }

        order.setStatus(OrderStatus.PAID);
        Order updatedOrder = orderRepository.save(order);
        return OrderMapper.INSTANCE.toDTO(updatedOrder);
    }

    @Transactional
    public OrderDTO shipOrder(Long orderId, Long sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("You are not authorized to ship this order");
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new RuntimeException("Order cannot be shipped");
        }

        order.setStatus(OrderStatus.SHIPPED);
        Order updatedOrder = orderRepository.save(order);
        return OrderMapper.INSTANCE.toDTO(updatedOrder);
    }

    @Transactional
    public OrderDTO confirmOrderDelivery(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new RuntimeException("You are not authorized to confirm this order's delivery");
        }

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new RuntimeException("Order delivery cannot be confirmed");
        }

        order.setStatus(OrderStatus.COMPLETED);
        Order updatedOrder = orderRepository.save(order);
        return OrderMapper.INSTANCE.toDTO(updatedOrder);
    }
}