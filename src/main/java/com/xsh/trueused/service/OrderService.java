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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xsh.trueused.dto.CreateOrderRequest;
import com.xsh.trueused.dto.OrderDTO;
import com.xsh.trueused.dto.ProductDTO;
import com.xsh.trueused.dto.ShipOrderRequest;
import com.xsh.trueused.dto.ShippingInfoDTO;
import com.xsh.trueused.entity.Address;
import com.xsh.trueused.entity.Order;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.entity.UserCoupon;
import com.xsh.trueused.enums.OrderStatus;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.mapper.OrderMapper;
import com.xsh.trueused.mapper.ProductMapper;
import com.xsh.trueused.repository.AddressRepository;
import com.xsh.trueused.repository.OrderRepository;
import com.xsh.trueused.repository.ProductRepository;
import com.xsh.trueused.repository.UserCouponRepository;
import com.xsh.trueused.repository.UserRepository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private ShippingService shippingService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ProductService productService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private OrderMapper orderMapper;

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest createOrderRequest, Long buyerId) {
        // 1. 查找商品
        Product product = productRepository.findById(createOrderRequest.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        // 2. 检查商品状态是否可购买
        if (product.getStatus() != ProductStatus.ON_SALE) {
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

        // 5. Handle Coupon
        java.math.BigDecimal discountAmount = java.math.BigDecimal.ZERO;
        if (createOrderRequest.getUserCouponId() != null) {
            UserCoupon userCoupon = userCouponRepository.findById(createOrderRequest.getUserCouponId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));

            if (!userCoupon.getUser().getId().equals(buyerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Coupon does not belong to you");
            }
            if (userCoupon.getIsUsed()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon already used");
            }
            if (userCoupon.getValidUntil() != null && userCoupon.getValidUntil().isBefore(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon expired");
            }
            if (product.getPrice().compareTo(userCoupon.getCoupon().getMinSpend()) < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Order amount does not meet coupon minimum spend");
            }

            discountAmount = userCoupon.getCoupon().getDiscountAmount();
            userCoupon.setIsUsed(true);
            userCoupon.setUsedAt(Instant.now());
            userCouponRepository.save(userCoupon);
        }

        // 6. 创建并保存订单
        Order order = new Order();
        order.setBuyer(buyer);
        order.setSeller(product.getSeller());
        order.setProduct(product);
        order.setAddress(address);

        java.math.BigDecimal finalPrice = product.getPrice().subtract(discountAmount);
        if (finalPrice.compareTo(java.math.BigDecimal.ZERO) < 0) {
            finalPrice = java.math.BigDecimal.ZERO;
        }
        order.setPrice(finalPrice);
        order.setDiscountAmount(discountAmount);

        // Create product snapshot
        try {
            ProductDTO productDTO = ProductMapper.toDTO(product);
            String snapshot = objectMapper.writeValueAsString(productDTO);
            order.setProductSnapshot(snapshot);
        } catch (Exception e) {
            log.error("Failed to create product snapshot for order", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create product snapshot");
        }

        order.setStatus(OrderStatus.PENDING_PAYMENT); // 初始状态为待付款

        Order savedOrder = orderRepository.save(order);

        // 7. 更新商品状态为已锁定
        productService.updateProductStatus(product.getId(), ProductStatus.LOCKED);

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

        return orderRepository.findAll(spec, pageable).map(orderMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return orderMapper.toDTO(order);
    }

    @Transactional
    public OrderDTO payOrder(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to pay for this order");
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be paid");
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        // 更新商品状态为已售出
        productService.updateProductStatus(order.getProduct().getId(), ProductStatus.SOLD_OUT);

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
    public OrderDTO payOrderByWallet(Long orderId, Long buyerId, String password) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to pay for this order");
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be paid");
        }

        // Deduct from buyer's wallet
        walletService.payOrder(buyerId, orderId, order.getPrice(), password);

        order.setStatus(OrderStatus.PAID);
        order.setPaymentTime(Instant.now());
        // order.setPaymentMethod("WALLET");
        orderRepository.save(order);

        // 更新商品状态为已售出
        productService.updateProductStatus(order.getProduct().getId(), ProductStatus.SOLD_OUT);

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
    public void handlePaymentSuccess(Long orderId, String transactionId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        // 幂等性检查：如果订单已经是支付状态，直接返回
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.COMPLETED) {
            log.info("Order {} is already paid, skipping update.", orderId);
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("Order {} status is {}, cannot be paid.", orderId, order.getStatus());
            // 这里可能需要根据业务决定是否抛出异常，或者记录异常日志
            return;
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentTime(Instant.now());
        order.setTransactionId(transactionId);
        orderRepository.save(order);

        // 更新商品状态为已售出
        productService.updateProductStatus(order.getProduct().getId(), ProductStatus.SOLD_OUT);

        log.info("Order {} payment success. Transaction ID: {}", orderId, transactionId);

        // 通知卖家买家已付款
        notificationService.createNotification(
                order.getSeller().getId(),
                "订单已付款",
                "订单 [" + order.getId() + "] 买家已付款，请尽快发货。",
                "ORDER_PAID",
                order.getId());
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

        ShippingInfoDTO shippingInfo = shippingService.getShippingInfo(order.getTrackingNumber());

        // 如果缓存中没有物流信息（可能是服务重启导致），尝试重建
        if (shippingInfo == null && order.getShippedAt() != null) {
            // 由于Order实体未持久化发货地信息，这里使用默认值
            // 在实际生产环境中，应该将senderCity等信息保存到Order表中
            String senderCity = "发货地";
            String senderDistrict = "";

            shippingInfo = shippingService.reconstructShippingInfo(
                    order.getTrackingNumber(),
                    order.getExpressCompany(),
                    order.getShippedAt(),
                    senderCity,
                    senderDistrict,
                    order.getAddress());
        }

        return shippingInfo;
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

        // Transfer money to seller
        walletService.transferToSeller(order.getSeller().getId(), orderId, order.getPrice());

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

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PAID) {
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
        productService.updateProductStatus(order.getProduct().getId(), ProductStatus.ON_SALE);

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

        // Refund to buyer
        walletService.refund(order.getBuyer().getId(), orderId, order.getPrice());

        productService.updateProductStatus(order.getProduct().getId(), ProductStatus.ON_SALE);

        return getOrderById(orderId);
    }

    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    @Transactional
    public void cancelExpiredOrders() {
        Instant expirationTime = Instant.now().minus(15, ChronoUnit.MINUTES);
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT,
                expirationTime);

        for (Order order : expiredOrders) {
            log.info("Cancelling expired order: {}", order.getId());
            order.setStatus(OrderStatus.CANCELLED);

            // 恢复商品库存/状态
            productService.updateProductStatus(order.getProduct().getId(), ProductStatus.ON_SALE);

            orderRepository.save(order);
        }
    }
}
