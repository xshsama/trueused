package com.xsh.trueused.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.xsh.trueused.dto.CreateOrderRequest;
import com.xsh.trueused.dto.OrderDTO;
import com.xsh.trueused.dto.ShipOrderRequest;
import com.xsh.trueused.dto.ShippingInfoDTO;
import com.xsh.trueused.entity.Address;
import com.xsh.trueused.entity.Order;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.enums.OrderStatus;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.mapper.OrderMapper;
import com.xsh.trueused.repository.AddressRepository;
import com.xsh.trueused.repository.OrderRepository;
import com.xsh.trueused.repository.ProductRepository;
import com.xsh.trueused.repository.UserRepository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ShippingService shippingService;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest createOrderRequest, Long buyerId) {
        // 1. 查找商品
        Product product = productRepository.findById(createOrderRequest.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        // 2. 检查商品状态是否可购买
        if (product.getStatus() != ProductStatus.AVAILABLE) {
            log.warn("CreateOrder conflict: productId={}, status={}, buyerId={}, sellerId={}",
                    product.getId(), product.getStatus(), buyerId,
                    product.getSeller() != null ? product.getSeller().getId() : null);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product is not available for purchase");
        }

        // 3. 查找买家
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));

        // 4. 查找地址并验证
        Address address = addressRepository.findById(createOrderRequest.getAddressId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        if (!address.getUser().getId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Address does not belong to the current user");
        }

        // 5. 不能购买自己的商品
        // if (product.getSeller().getId().equals(buyerId)) {
        // log.warn("CreateOrder bad request: buyer {} attempts to buy own product {}",
        // buyerId, product.getId());
        // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot buy
        // your own product");
        // }

        // 6. 创建并保存订单
        Order order = new Order();
        order.setBuyer(buyer);
        order.setSeller(product.getSeller());
        order.setProduct(product);
        order.setAddress(address);
        order.setPrice(product.getPrice());
        order.setStatus(OrderStatus.PENDING); // 初始状态为待处理

        Order savedOrder = orderRepository.save(order);

        // 7. 更新商品状态为已售出
        product.setStatus(ProductStatus.SOLD);
        productRepository.save(product);

        // 通知卖家有新订单
        notificationService.createNotification(
                product.getSeller().getId(),
                "新订单提醒",
                "您的商品 [" + product.getTitle() + "] 有新的订单，请等待买家付款。",
                "ORDER_CREATED",
                savedOrder.getId());

        // 8. 转换并返回 DTO
        return getOrderById(savedOrder.getId());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByBuyer(Long buyerId) {
        return orderRepository.findByBuyerId(buyerId).stream()
                .map(OrderMapper.INSTANCE::toDTO)
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

        return orderRepository.findAll(spec, pageable).map(OrderMapper.INSTANCE::toDTO);
    }

    @Transactional(readOnly = true)
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
        orderRepository.save(order);

        // 通知卖家买家已付款
        notificationService.createNotification(
                order.getSeller().getId(),
                "订单已付款",
                "订单 [" + order.getId() + "] 买家已付款，请尽快发货。",
                "ORDER_PAID",
                order.getId());

        return getOrderById(orderId);
    }

    @Transactional
    public OrderDTO shipOrder(Long orderId, Long sellerId, ShipOrderRequest shipRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getSeller().getId().equals(sellerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to ship this order");
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be shipped");
        }

        // 获取快递公司，如果请求中没有提供则使用默认值
        String expressCompany = (shipRequest != null && shipRequest.getExpressCompany() != null)
                ? shipRequest.getExpressCompany()
                : "顺丰速运";
        String trackingNumber = (shipRequest != null) ? shipRequest.getTrackingNumber() : null;

        // 获取发货地址信息
        String senderCity = (shipRequest != null && shipRequest.getSenderCity() != null)
                ? shipRequest.getSenderCity()
                : "发货地";
        String senderDistrict = (shipRequest != null && shipRequest.getSenderDistrict() != null)
                ? shipRequest.getSenderDistrict()
                : "";

        // 获取买家收货地址
        Address receiverAddress = order.getAddress();

        // 调用物流服务创建快递订单
        ShippingInfoDTO shippingInfo = shippingService.createShippingOrder(
                expressCompany,
                trackingNumber,
                senderCity,
                senderDistrict,
                receiverAddress);

        // 更新订单物流信息
        order.setTrackingNumber(shippingInfo.getTrackingNumber());
        order.setExpressCompany(shippingInfo.getExpressCompany());
        order.setExpressCode(shippingInfo.getExpressCode());
        order.setShippedAt(shippingInfo.getShippedAt());
        order.setEstimatedDeliveryTime(shippingInfo.getEstimatedDeliveryTime());
        order.setStatus(OrderStatus.SHIPPED);

        orderRepository.save(order);

        // 通知买家已发货
        notificationService.createNotification(
                order.getBuyer().getId(),
                "订单已发货",
                "您的订单 [" + order.getId() + "] 卖家已发货，快递单号：" + order.getTrackingNumber(),
                "ORDER_SHIPPED",
                order.getId());

        // 返回包含物流信息的订单DTO
        OrderDTO orderDTO = getOrderById(orderId);
        orderDTO.setShippingInfo(shippingInfo);
        return orderDTO;
    }

    /**
     * 获取订单物流追踪信息
     */
    @Transactional(readOnly = true)
    public ShippingInfoDTO getOrderShippingInfo(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getTrackingNumber() == null) {
            return null;
        }

        return shippingService.getShippingInfo(order.getTrackingNumber());
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
        order.setDeliveredAt(Instant.now());
        orderRepository.save(order);

        // 通知卖家订单已完成
        notificationService.createNotification(
                order.getSeller().getId(),
                "订单已完成",
                "订单 [" + order.getId() + "] 买家已确认收货，交易完成。",
                "ORDER_COMPLETED",
                order.getId());

        // TODO: Archive the product after order completion
        Product product = order.getProduct();
        System.out
                .println("Order " + order.getId() + " completed. Product " + product.getId() + " should be archived.");

        return getOrderById(orderId);
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
        orderRepository.save(order);

        // 将商品状态恢复为可购买
        Product product = order.getProduct();
        product.setStatus(ProductStatus.AVAILABLE);
        productRepository.save(product);

        // 通知对方订单已取消
        Long targetUserId = order.getBuyer().getId().equals(userId) ? order.getSeller().getId()
                : order.getBuyer().getId();
        String canceller = order.getBuyer().getId().equals(userId) ? "买家" : "卖家";

        notificationService.createNotification(
                targetUserId,
                "订单已取消",
                canceller + "取消了订单 [" + order.getId() + "]。",
                "ORDER_CANCELLED",
                order.getId());

        return getOrderById(orderId);
    }

    @Transactional
    public OrderDTO refundOrder(Long orderId, Long sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getSeller().getId().equals(sellerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to refund this order");
        }

        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.SHIPPED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be refunded at its current stage");
        }

        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);

        Product product = order.getProduct();
        product.setStatus(ProductStatus.AVAILABLE);
        productRepository.save(product);

        return getOrderById(orderId);
    }

    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    @Transactional
    public void cancelExpiredOrders() {
        Instant expirationTime = Instant.now().minus(15, ChronoUnit.MINUTES);
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, expirationTime);

        for (Order order : expiredOrders) {
            log.info("Cancelling expired order: {}", order.getId());
            order.setStatus(OrderStatus.CANCELLED);

            // 恢复商品库存/状态
            Product product = order.getProduct();
            product.setStatus(ProductStatus.AVAILABLE);
            productRepository.save(product);

            orderRepository.save(order);
        }
    }
}
