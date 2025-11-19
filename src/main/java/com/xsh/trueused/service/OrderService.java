package com.xsh.trueused.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        // 2. 检查商品状态是否可购买
        if (product.getStatus() != ProductStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product is not available for purchase");
        }

        // 3. 查找买家
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));

        // 4. 不能购买自己的商品
        if (product.getSeller().getId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot buy your own product");
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return OrderMapper.INSTANCE.toDTO(order);
    }

    @Transactional
    public OrderDTO payOrder(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to pay for this order");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be paid");
        }

        order.setStatus(OrderStatus.PAID);
        Order updatedOrder = orderRepository.save(order);
        return OrderMapper.INSTANCE.toDTO(updatedOrder);
    }

    @Transactional
    public OrderDTO shipOrder(Long orderId, Long sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getSeller().getId().equals(sellerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to ship this order");
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be shipped");
        }

        order.setStatus(OrderStatus.SHIPPED);
        Order updatedOrder = orderRepository.save(order);
        return OrderMapper.INSTANCE.toDTO(updatedOrder);
    }

    @Transactional
    public OrderDTO confirmOrderDelivery(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to confirm this order's delivery");
        }

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order delivery cannot be confirmed");
        }

        order.setStatus(OrderStatus.COMPLETED);
        Order updatedOrder = orderRepository.save(order);

        // TODO: Archive the product after order completion
        Product product = order.getProduct();
        System.out
                .println("Order " + order.getId() + " completed. Product " + product.getId() + " should be archived.");

        return OrderMapper.INSTANCE.toDTO(updatedOrder);
    }

    @Transactional
    public OrderDTO cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        // 允许买家在付款前取消，或卖家取消
        if (!order.getBuyer().getId().equals(userId) && !order.getSeller().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to cancel this order");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be cancelled at its current stage");
        }

        // 如果是卖家取消已付款的订单，理论上应该有退款流程，这里简化处理
        if (order.getSeller().getId().equals(userId) && order.getStatus() == OrderStatus.PAID) {
            // TODO: Implement refund logic here
            System.out.println("Refund process should be triggered for order: " + orderId);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);

        // 将商品状态恢复为可购买
        Product product = order.getProduct();
        product.setStatus(ProductStatus.AVAILABLE);
        productRepository.save(product);

        return OrderMapper.INSTANCE.toDTO(updatedOrder);
    }
}