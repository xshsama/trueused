package com.xsh.trueused.order.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xsh.trueused.order.dto.CreateOrderRequest;
import com.xsh.trueused.order.dto.OrderDTO;
import com.xsh.trueused.product.dto.ProductDTO;
import com.xsh.trueused.order.dto.ShipOrderRequest;
import com.xsh.trueused.order.dto.ShippingInfoDTO;
import com.xsh.trueused.entity.Address;
import com.xsh.trueused.entity.Order;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.entity.UserCoupon;
import com.xsh.trueused.order.enums.OrderStatus;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.product.mapper.ProductMapper;
import com.xsh.trueused.address.repository.AddressRepository;
import com.xsh.trueused.order.repository.OrderRepository;
import com.xsh.trueused.product.repository.ProductRepository;
import com.xsh.trueused.coupon.repository.UserCouponRepository;
import com.xsh.trueused.user.repository.UserRepository;
import com.xsh.trueused.order.state.OrderStateMachine;
import com.xsh.trueused.order.state.OrderTransition;
import com.xsh.trueused.order.payment.OrderPaymentContext;
import com.xsh.trueused.order.payment.OrderPaymentResult;
import com.xsh.trueused.order.payment.OrderPaymentStrategyFactory;
import com.xsh.trueused.order.payment.OrderPaymentType;
import com.xsh.trueused.notification.service.NotificationService;
import com.xsh.trueused.product.service.ProductService;
import com.xsh.trueused.wallet.service.WalletService;

@Service
public class OrderCommandService {

    private static final Logger log = LoggerFactory.getLogger(OrderCommandService.class);

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
    private OrderStateMachine orderStateMachine;

    @Autowired
    private OrderPaymentStrategyFactory orderPaymentStrategyFactory;

    @Autowired
    private OrderQueryService orderQueryService;

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
        return orderQueryService.getOrderById(savedOrder.getId());
    }

    @Transactional
    public OrderDTO payOrder(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to pay for this order");
        }

        orderPaymentStrategyFactory.get(OrderPaymentType.DIRECT)
                .execute(order, OrderPaymentContext.direct(buyerId));

        return orderQueryService.getOrderById(orderId);
    }

    @Transactional
    public OrderDTO payOrderByWallet(Long orderId, Long buyerId, String password) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to pay for this order");
        }

        orderPaymentStrategyFactory.get(OrderPaymentType.WALLET)
                .execute(order, OrderPaymentContext.wallet(buyerId, password));

        return orderQueryService.getOrderById(orderId);
    }

    @Transactional
    public void handlePaymentSuccess(Long orderId, String transactionId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        OrderPaymentResult result = orderPaymentStrategyFactory.get(OrderPaymentType.CALLBACK)
                .execute(order, OrderPaymentContext.callback(transactionId));
        if (result == OrderPaymentResult.PROCESSED) {
            log.info("Order {} payment success. Transaction ID: {}", orderId, transactionId);
        }
    }

    @Transactional
    public OrderDTO shipOrder(Long orderId, Long sellerId, ShipOrderRequest shipRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getSeller().getId().equals(sellerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to ship this order");
        }

        orderStateMachine.assertCanTransit(order.getStatus(), OrderTransition.SHIP, "Order cannot be shipped");

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
        order.setStatus(orderStateMachine.nextStatus(order.getStatus(), OrderTransition.SHIP));

        orderRepository.save(order);

        // 通知买家已发货
        notificationService.createNotification(
                order.getBuyer().getId(),
                "订单已发货",
                "您的订单 [" + order.getId() + "] 卖家已发货，快递单号：" + order.getTrackingNumber(),
                "ORDER_SHIPPED",
                order.getId());

        // 返回包含物流信息的订单DTO
        OrderDTO orderDTO = orderQueryService.getOrderById(orderId);
        orderDTO.setShippingInfo(shippingInfo);
        return orderDTO;
    }

    @Transactional
    public OrderDTO confirmOrderDelivery(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to confirm this order's delivery");
        }

        orderStateMachine.assertCanTransit(order.getStatus(), OrderTransition.CONFIRM_DELIVERY,
                "Order delivery cannot be confirmed");
        order.setStatus(orderStateMachine.nextStatus(order.getStatus(), OrderTransition.CONFIRM_DELIVERY));
        order.setDeliveredAt(Instant.now());
        orderRepository.save(order);

        // Transfer money to seller
        walletService.transferToSeller(order.getSeller().getId(), order.getBuyer().getId(), orderId, order.getPrice());

        // 通知卖家订单已完成
        notificationService.createNotification(
                order.getSeller().getId(),
                "订单已完成",
                "订单 [" + order.getId() + "] 买家已确认收货，交易完成。",
                "ORDER_COMPLETED",
                order.getId());

        // TODO: Archive the product after order completion
        Product product = order.getProduct();
        log.info("Order {} completed. Product {} should be archived.", order.getId(), product.getId());

        return orderQueryService.getOrderById(orderId);
    }

    @Transactional
    public OrderDTO cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        // 允许买家在付款前取消，或卖家取消
        if (!order.getBuyer().getId().equals(userId) && !order.getSeller().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to cancel this order");
        }

        orderStateMachine.assertCanTransit(order.getStatus(), OrderTransition.CANCEL,
                "Order cannot be cancelled at its current stage");

        // 已付款订单取消时，退款并释放买家冻结资金
        if (order.getStatus() == OrderStatus.PAID) {
            walletService.refund(order.getBuyer().getId(), orderId, order.getPrice());
        }

        order.setStatus(orderStateMachine.nextStatus(order.getStatus(), OrderTransition.CANCEL));
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

        return orderQueryService.getOrderById(orderId);
    }

    @Transactional
    public OrderDTO refundOrder(Long orderId, Long sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getSeller().getId().equals(sellerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to refund this order");
        }

        orderStateMachine.assertCanTransit(order.getStatus(), OrderTransition.REFUND,
                "Order cannot be refunded at its current stage");
        order.setStatus(orderStateMachine.nextStatus(order.getStatus(), OrderTransition.REFUND));
        orderRepository.save(order);

        // Refund to buyer
        walletService.refund(order.getBuyer().getId(), orderId, order.getPrice());

        productService.updateProductStatus(order.getProduct().getId(), ProductStatus.ON_SALE);

        // Notify buyer
        notificationService.createNotification(
                order.getBuyer().getId(),
                "订单已退款",
                "您的订单 [" + order.getId() + "] 卖家已发起退款，金额将退回您的钱包。",
                "ORDER_REFUNDED",
                order.getId());

        return orderQueryService.getOrderById(orderId);
    }

    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    @Transactional
    public void cancelExpiredOrders() {
        Instant expirationTime = Instant.now().minus(15, ChronoUnit.MINUTES);
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT,
                expirationTime);

        for (Order order : expiredOrders) {
            if (!orderStateMachine.canTransit(order.getStatus(), OrderTransition.EXPIRE)) {
                log.info("Skip expiring order {} due to current status {}", order.getId(), order.getStatus());
                continue;
            }

            log.info("Cancelling expired order: {}", order.getId());
            order.setStatus(orderStateMachine.nextStatus(order.getStatus(), OrderTransition.EXPIRE));

            // 恢复商品库存/状态
            productService.updateProductStatus(order.getProduct().getId(), ProductStatus.ON_SALE);

            orderRepository.save(order);

            // Notify buyer
            notificationService.createNotification(
                    order.getBuyer().getId(),
                    "订单已取消",
                    "您的订单 [" + order.getId() + "] 因超时未支付已自动取消。",
                    "ORDER_CANCELLED",
                    order.getId());
        }
    }
}
