package com.xsh.trueused.order.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
import com.xsh.trueused.enums.ProductTradeModel;
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
import com.xsh.trueused.observability.metrics.BusinessMetricsRecorder;
import com.xsh.trueused.product.service.ProductService;
import com.xsh.trueused.wallet.service.WalletService;

import io.micrometer.core.instrument.Timer;

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

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BusinessMetricsRecorder businessMetricsRecorder;

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest createOrderRequest, Long buyerId) {
        Timer.Sample sample = businessMetricsRecorder.startCommandSample();
        ProductTradeModel tradeModel = null;
        String timerResult = "success";

        try {
            // 1. 查找商品
            Product product = productRepository.findById(createOrderRequest.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
            tradeModel = product.getTradeModel();

            // 2. 检查商品状态是否可购买
            if (product.getStatus() != ProductStatus.ON_SALE) {
                log.warn("CreateOrder conflict: productId={}, status={}, buyerId={}, sellerId={}",
                        product.getId(), product.getStatus(), buyerId,
                        product.getSeller() != null ? product.getSeller().getId() : null);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Product is not available for purchase");
            }
            if (product.getSeller() != null && product.getSeller().getId().equals(buyerId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Buyer cannot purchase own product");
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

            businessMetricsRecorder.recordOrderCreated(
                    businessMetricsRecorder.normalize(tradeModel),
                    "success",
                    "none",
                    savedOrder.getPrice());
            return orderQueryService.getOrderById(savedOrder.getId());
        } catch (ResponseStatusException ex) {
            timerResult = "error";
            businessMetricsRecorder.recordOrderCreated(
                    businessMetricsRecorder.normalize(tradeModel),
                    "error",
                    classifyStatusReason(ex),
                    null);
            throw ex;
        } catch (RuntimeException ex) {
            timerResult = "error";
            businessMetricsRecorder.recordOrderCreated(
                    businessMetricsRecorder.normalize(tradeModel),
                    "error",
                    "internal",
                    null);
            throw ex;
        } finally {
            businessMetricsRecorder.stopCommandSample("create_order", timerResult, sample);
        }
    }

    @Transactional
    public OrderDTO payOrder(Long orderId, Long buyerId) {
        Timer.Sample sample = businessMetricsRecorder.startCommandSample();
        String timerResult = "success";
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            if (!order.getBuyer().getId().equals(buyerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to pay for this order");
            }

            OrderPaymentResult result = orderPaymentStrategyFactory.get(OrderPaymentType.DIRECT)
                    .execute(order, OrderPaymentContext.direct(buyerId));

            if (result == OrderPaymentResult.PROCESSED) {
                maybeSchedulePlatformShipment(order);
            }

            businessMetricsRecorder.recordPayment("direct", businessMetricsRecorder.normalize(result), order.getPrice());
            return orderQueryService.getOrderById(orderId);
        } catch (RuntimeException ex) {
            timerResult = "error";
            businessMetricsRecorder.recordPayment("direct", "error", null);
            throw ex;
        } finally {
            businessMetricsRecorder.stopCommandSample("pay_order_direct", timerResult, sample);
        }
    }

    @Transactional
    public OrderDTO payOrderByWallet(Long orderId, Long buyerId, String password) {
        Timer.Sample sample = businessMetricsRecorder.startCommandSample();
        String timerResult = "success";
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            if (!order.getBuyer().getId().equals(buyerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to pay for this order");
            }

            OrderPaymentResult result = orderPaymentStrategyFactory.get(OrderPaymentType.WALLET)
                    .execute(order, OrderPaymentContext.wallet(buyerId, password));

            if (result == OrderPaymentResult.PROCESSED) {
                maybeSchedulePlatformShipment(order);
            }

            businessMetricsRecorder.recordPayment("wallet", businessMetricsRecorder.normalize(result), order.getPrice());
            return orderQueryService.getOrderById(orderId);
        } catch (RuntimeException ex) {
            timerResult = "error";
            businessMetricsRecorder.recordPayment("wallet", "error", null);
            throw ex;
        } finally {
            businessMetricsRecorder.stopCommandSample("pay_order_wallet", timerResult, sample);
        }
    }

    @Transactional
    public void handlePaymentSuccess(Long orderId, String transactionId) {
        Timer.Sample sample = businessMetricsRecorder.startCommandSample();
        String timerResult = "success";
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

            OrderPaymentResult result = orderPaymentStrategyFactory.get(OrderPaymentType.CALLBACK)
                    .execute(order, OrderPaymentContext.callback(transactionId));
            if (result == OrderPaymentResult.PROCESSED) {
                log.info("Order {} payment success. Transaction ID: {}", orderId, transactionId);
                maybeSchedulePlatformShipment(order);
            }

            String normalizedResult = businessMetricsRecorder.normalize(result);
            businessMetricsRecorder.recordPayment("callback", normalizedResult, order.getPrice());
            businessMetricsRecorder.recordPaymentCallback(normalizedResult);
        } catch (RuntimeException ex) {
            timerResult = "error";
            businessMetricsRecorder.recordPayment("callback", "error", null);
            businessMetricsRecorder.recordPaymentCallback("error");
            throw ex;
        } finally {
            businessMetricsRecorder.stopCommandSample("payment_callback", timerResult, sample);
        }
    }

    @Transactional
    public OrderDTO shipOrder(Long orderId, Long sellerId, ShipOrderRequest shipRequest) {
        Timer.Sample sample = businessMetricsRecorder.startCommandSample();
        String timerResult = "success";
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

            if (!order.getSeller().getId().equals(sellerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to ship this order");
            }

            if (isOfficialInspectionOrder(order)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Official inspection orders are fulfilled by platform");
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
                    "SELLER_OUTBOUND",
                    senderCity,
                    senderDistrict,
                    receiverAddress);

            applyShippingToOrder(order, shippingInfo);

            orderRepository.save(order);

            // 通知买家已发货
            notificationService.createNotification(
                    order.getBuyer().getId(),
                    "订单已发货",
                    "您的订单 [" + order.getId() + "] 卖家已发货，快递单号：" + order.getTrackingNumber(),
                    "ORDER_SHIPPED",
                    order.getId());

            OrderDTO orderDTO = orderQueryService.getOrderById(orderId);
            orderDTO.setShippingInfo(shippingInfo);
            businessMetricsRecorder.recordShipment("seller", "success");
            return orderDTO;
        } catch (RuntimeException ex) {
            timerResult = "error";
            businessMetricsRecorder.recordShipment("seller", "error");
            throw ex;
        } finally {
            businessMetricsRecorder.stopCommandSample("ship_order_seller", timerResult, sample);
        }
    }

    @Transactional
    public OrderDTO platformShipOrder(Long orderId) {
        Timer.Sample sample = businessMetricsRecorder.startCommandSample();
        String timerResult = "success";
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

            if (!isOfficialInspectionOrder(order)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Only official inspection orders can be platform shipped");
            }

            if (order.getStatus() != OrderStatus.PENDING_SHIPMENT) {
                timerResult = "skipped";
                businessMetricsRecorder.recordShipment("platform", "skipped");
                return orderQueryService.getOrderById(orderId);
            }

            ShippingInfoDTO shippingInfo = shippingService.createShippingOrder(
                    "京东物流",
                    null,
                    "PLATFORM_OUTBOUND",
                    "平台仓",
                    "质检仓",
                    order.getAddress());

            applyShippingToOrder(order, shippingInfo);
            orderRepository.save(order);

            notificationService.createNotification(
                    order.getBuyer().getId(),
                    "平台已出库",
                    "订单 [" + order.getId() + "] 已由平台仓出库，快递单号：" + order.getTrackingNumber(),
                    "ORDER_SHIPPED",
                    order.getId());

            businessMetricsRecorder.recordShipment("platform", "success");
            return orderQueryService.getOrderById(orderId);
        } catch (RuntimeException ex) {
            timerResult = "error";
            businessMetricsRecorder.recordShipment("platform", "error");
            throw ex;
        } finally {
            businessMetricsRecorder.stopCommandSample("ship_order_platform", timerResult, sample);
        }
    }

    @Transactional
    public OrderDTO confirmOrderDelivery(Long orderId, Long buyerId) {
        Timer.Sample sample = businessMetricsRecorder.startCommandSample();
        String timerResult = "success";
        OrderStatus fromStatus = null;
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
            fromStatus = order.getStatus();

            if (!order.getBuyer().getId().equals(buyerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not authorized to confirm this order's delivery");
            }

            orderStateMachine.assertCanTransit(order.getStatus(), OrderTransition.CONFIRM_DELIVERY,
                    "Order delivery cannot be confirmed");
            assertShippingReadyForDelivery(order);
            OrderStatus toStatus = orderStateMachine.nextStatus(order.getStatus(), OrderTransition.CONFIRM_DELIVERY);
            order.setStatus(toStatus);
            order.setDeliveredAt(Instant.now());
            orderRepository.save(order);

            walletService.transferToSeller(order.getSeller().getId(), order.getBuyer().getId(), orderId, order.getPrice());

            notificationService.createNotification(
                    order.getSeller().getId(),
                    "订单已完成",
                    "订单 [" + order.getId() + "] 买家已确认收货，交易完成。",
                    "ORDER_COMPLETED",
                    order.getId());

            Product product = order.getProduct();
            log.info("Order {} completed. Product {} should be archived.", order.getId(), product.getId());

            businessMetricsRecorder.recordOrderTransition("confirm_delivery",
                    businessMetricsRecorder.normalize(fromStatus),
                    businessMetricsRecorder.normalize(toStatus),
                    "success",
                    "none");
            return orderQueryService.getOrderById(orderId);
        } catch (ResponseStatusException ex) {
            timerResult = "error";
            businessMetricsRecorder.recordOrderTransition("confirm_delivery",
                    businessMetricsRecorder.normalize(fromStatus),
                    "unknown",
                    "error",
                    classifyStatusReason(ex));
            throw ex;
        } catch (RuntimeException ex) {
            timerResult = "error";
            businessMetricsRecorder.recordOrderTransition("confirm_delivery",
                    businessMetricsRecorder.normalize(fromStatus),
                    "unknown",
                    "error",
                    "internal");
            throw ex;
        } finally {
            businessMetricsRecorder.stopCommandSample("confirm_delivery", timerResult, sample);
        }
    }

    @Transactional
    public OrderDTO cancelOrder(Long orderId, Long userId) {
        Timer.Sample sample = businessMetricsRecorder.startCommandSample();
        String timerResult = "success";
        OrderStatus fromStatus = null;
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
            fromStatus = order.getStatus();

            if (!order.getBuyer().getId().equals(userId) && !order.getSeller().getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to cancel this order");
            }

            orderStateMachine.assertCanTransit(order.getStatus(), OrderTransition.CANCEL,
                    "Order cannot be cancelled at its current stage");

            if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.PENDING_SHIPMENT) {
                walletService.refund(order.getBuyer().getId(), orderId, order.getPrice());
            }

            OrderStatus toStatus = orderStateMachine.nextStatus(order.getStatus(), OrderTransition.CANCEL);
            order.setStatus(toStatus);
            orderRepository.save(order);

            productService.updateProductStatus(order.getProduct().getId(), ProductStatus.ON_SALE);

            Long targetUserId = order.getBuyer().getId().equals(userId) ? order.getSeller().getId()
                    : order.getBuyer().getId();
            String canceller = order.getBuyer().getId().equals(userId) ? "买家" : "卖家";

            notificationService.createNotification(
                    targetUserId,
                    "订单已取消",
                    canceller + "取消了订单 [" + order.getId() + "]。",
                    "ORDER_CANCELLED",
                    order.getId());

            businessMetricsRecorder.recordOrderTransition("cancel_order",
                    businessMetricsRecorder.normalize(fromStatus),
                    businessMetricsRecorder.normalize(toStatus),
                    "success",
                    "none");
            return orderQueryService.getOrderById(orderId);
        } catch (ResponseStatusException ex) {
            timerResult = "error";
            businessMetricsRecorder.recordOrderTransition("cancel_order",
                    businessMetricsRecorder.normalize(fromStatus),
                    "unknown",
                    "error",
                    classifyStatusReason(ex));
            throw ex;
        } catch (RuntimeException ex) {
            timerResult = "error";
            businessMetricsRecorder.recordOrderTransition("cancel_order",
                    businessMetricsRecorder.normalize(fromStatus),
                    "unknown",
                    "error",
                    "internal");
            throw ex;
        } finally {
            businessMetricsRecorder.stopCommandSample("cancel_order", timerResult, sample);
        }
    }

    @Transactional
    public OrderDTO refundOrder(Long orderId, Long sellerId) {
        Timer.Sample sample = businessMetricsRecorder.startCommandSample();
        String timerResult = "success";
        OrderStatus fromStatus = null;
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
            fromStatus = order.getStatus();

            if (!order.getSeller().getId().equals(sellerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to refund this order");
            }

            orderStateMachine.assertCanTransit(order.getStatus(), OrderTransition.REFUND,
                    "Order cannot be refunded at its current stage");
            OrderStatus toStatus = orderStateMachine.nextStatus(order.getStatus(), OrderTransition.REFUND);
            order.setStatus(toStatus);
            orderRepository.save(order);

            walletService.refund(order.getBuyer().getId(), orderId, order.getPrice());

            productService.updateProductStatus(order.getProduct().getId(), ProductStatus.ON_SALE);

            notificationService.createNotification(
                    order.getBuyer().getId(),
                    "订单已退款",
                    "您的订单 [" + order.getId() + "] 卖家已发起退款，金额将退回您的钱包。",
                    "ORDER_REFUNDED",
                    order.getId());

            businessMetricsRecorder.recordOrderTransition("refund_order",
                    businessMetricsRecorder.normalize(fromStatus),
                    businessMetricsRecorder.normalize(toStatus),
                    "success",
                    "none");
            return orderQueryService.getOrderById(orderId);
        } catch (ResponseStatusException ex) {
            timerResult = "error";
            businessMetricsRecorder.recordOrderTransition("refund_order",
                    businessMetricsRecorder.normalize(fromStatus),
                    "unknown",
                    "error",
                    classifyStatusReason(ex));
            throw ex;
        } catch (RuntimeException ex) {
            timerResult = "error";
            businessMetricsRecorder.recordOrderTransition("refund_order",
                    businessMetricsRecorder.normalize(fromStatus),
                    "unknown",
                    "error",
                    "internal");
            throw ex;
        } finally {
            businessMetricsRecorder.stopCommandSample("refund_order", timerResult, sample);
        }
    }

    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    @Transactional
    public void cancelExpiredOrders() {
        int affectedCount = 0;
        try {
            Instant expirationTime = Instant.now().minus(15, ChronoUnit.MINUTES);
            List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT,
                    expirationTime);

            for (Order order : expiredOrders) {
                if (!orderStateMachine.canTransit(order.getStatus(), OrderTransition.EXPIRE)) {
                    log.info("Skip expiring order {} due to current status {}", order.getId(), order.getStatus());
                    continue;
                }

                OrderStatus fromStatus = order.getStatus();
                OrderStatus toStatus = orderStateMachine.nextStatus(order.getStatus(), OrderTransition.EXPIRE);
                log.info("Cancelling expired order: {}", order.getId());
                order.setStatus(toStatus);

                productService.updateProductStatus(order.getProduct().getId(), ProductStatus.ON_SALE);

                orderRepository.save(order);
                affectedCount++;
                businessMetricsRecorder.recordOrderTransition("expire_order",
                        businessMetricsRecorder.normalize(fromStatus),
                        businessMetricsRecorder.normalize(toStatus),
                        "success",
                        "scheduled");

                notificationService.createNotification(
                        order.getBuyer().getId(),
                        "订单已取消",
                        "您的订单 [" + order.getId() + "] 因超时未支付已自动取消。",
                        "ORDER_CANCELLED",
                        order.getId());
            }
            businessMetricsRecorder.recordScheduledJob("cancel_expired_orders", "success", affectedCount);
        } catch (RuntimeException ex) {
            businessMetricsRecorder.recordScheduledJob("cancel_expired_orders", "error", affectedCount);
            throw ex;
        }
    }

    @Scheduled(fixedRate = 300000) // 每5分钟检查一次
    @Transactional
    public void autoCompleteShippedOrders() {
        int affectedCount = 0;
        try {
            Instant autoConfirmDeadline = Instant.now().minus(7, ChronoUnit.DAYS);
            List<Order> overdueShippedOrders = orderRepository.findByStatusAndShippedAtBefore(
                    OrderStatus.SHIPPED,
                    autoConfirmDeadline);

            for (Order order : overdueShippedOrders) {
                if (!orderStateMachine.canTransit(order.getStatus(), OrderTransition.CONFIRM_DELIVERY)) {
                    log.info("Skip auto confirm for order {} due to status {}", order.getId(), order.getStatus());
                    continue;
                }

                OrderStatus fromStatus = order.getStatus();
                OrderStatus toStatus = orderStateMachine.nextStatus(order.getStatus(), OrderTransition.CONFIRM_DELIVERY);
                order.setStatus(toStatus);
                order.setDeliveredAt(Instant.now());
                orderRepository.save(order);
                affectedCount++;
                businessMetricsRecorder.recordOrderTransition("auto_confirm_delivery",
                        businessMetricsRecorder.normalize(fromStatus),
                        businessMetricsRecorder.normalize(toStatus),
                        "success",
                        "scheduled");

                walletService.transferToSeller(order.getSeller().getId(), order.getBuyer().getId(), order.getId(),
                        order.getPrice());

                notificationService.createNotification(
                        order.getBuyer().getId(),
                        "系统自动确认收货",
                        "订单 [" + order.getId() + "] 已超过自动确认时限，系统已自动确认收货。",
                        "ORDER_AUTO_COMPLETED",
                        order.getId());

                notificationService.createNotification(
                        order.getSeller().getId(),
                        "订单自动完成",
                        "订单 [" + order.getId() + "] 已自动确认收货，款项已结算到您的钱包。",
                        "ORDER_COMPLETED",
                        order.getId());
            }
            businessMetricsRecorder.recordScheduledJob("auto_complete_shipped_orders", "success", affectedCount);
        } catch (RuntimeException ex) {
            businessMetricsRecorder.recordScheduledJob("auto_complete_shipped_orders", "error", affectedCount);
            throw ex;
        }
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void autoDispatchPendingPlatformOrders() {
        int affectedCount = 0;
        String result = "success";
        Instant dispatchDeadline = Instant.now().minus(5, ChronoUnit.SECONDS);
        List<Order> pendingPlatformOrders = orderRepository.findByStatusAndPaymentTimeBefore(
                OrderStatus.PENDING_SHIPMENT,
                dispatchDeadline);

        for (Order order : pendingPlatformOrders) {
            if (!isOfficialInspectionOrder(order)) {
                continue;
            }
            try {
                platformShipOrder(order.getId());
                affectedCount++;
            } catch (Exception e) {
                result = "partial_error";
                log.warn("Auto platform ship failed for order {}", order.getId(), e);
            }
        }
        businessMetricsRecorder.recordScheduledJob("auto_dispatch_platform_orders", result, affectedCount);
    }

    private String classifyStatusReason(ResponseStatusException ex) {
        return switch (ex.getStatusCode().value()) {
            case 400 -> "bad_request";
            case 403 -> "forbidden";
            case 404 -> "not_found";
            case 409 -> "conflict";
            default -> "error";
        };
    }

    private void maybeSchedulePlatformShipment(Order order) {
        if (!isOfficialInspectionOrder(order) || order.getStatus() != OrderStatus.PENDING_SHIPMENT) {
            return;
        }

        Long orderId = order.getId();
        CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
            try {
                OrderCommandService self = applicationContext.getBean(OrderCommandService.class);
                self.platformShipOrder(orderId);
            } catch (Exception e) {
                log.warn("Delayed platform ship failed for order {}", orderId, e);
            }
        });
    }

    private boolean isOfficialInspectionOrder(Order order) {
        return order.getProduct() != null && order.getProduct().getTradeModel() == ProductTradeModel.OFFICIAL_INSPECTION;
    }

    private void applyShippingToOrder(Order order, ShippingInfoDTO shippingInfo) {
        order.setTrackingNumber(shippingInfo.getTrackingNumber());
        order.setExpressCompany(shippingInfo.getExpressCompany());
        order.setExpressCode(shippingInfo.getExpressCode());
        order.setShippedAt(shippingInfo.getShippedAt());
        order.setEstimatedDeliveryTime(shippingInfo.getEstimatedDeliveryTime());
        order.setShippingSnapshot(writeShippingSnapshot(shippingInfo));
        order.setStatus(orderStateMachine.nextStatus(order.getStatus(), OrderTransition.SHIP));
    }

    private void assertShippingReadyForDelivery(Order order) {
        try {
            ShippingInfoDTO current = loadCurrentShippingInfo(order);
            if (current == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Shipping info is not ready yet");
            }
            if (!"DELIVERING".equals(current.getShippingStatus()) && !"DELIVERED".equals(current.getShippingStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Package is not ready for delivery confirmation yet");
            }
            order.setShippingSnapshot(writeShippingSnapshot(current));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read shipping snapshot");
        }
    }

    private ShippingInfoDTO loadCurrentShippingInfo(Order order) throws Exception {
        if (order.getShippingSnapshot() != null && !order.getShippingSnapshot().isBlank()) {
            ShippingInfoDTO snapshot = objectMapper.readValue(order.getShippingSnapshot(), ShippingInfoDTO.class);
            return shippingService.refreshShippingInfo(snapshot);
        }

        if (order.getTrackingNumber() == null || order.getShippedAt() == null) {
            return null;
        }

        return shippingService.reconstructShippingInfo(
                order.getTrackingNumber(),
                order.getExpressCompany(),
                order.getShippedAt(),
                isOfficialInspectionOrder(order) ? "PLATFORM_OUTBOUND" : "SELLER_OUTBOUND",
                isOfficialInspectionOrder(order) ? "平台仓" : "发货地",
                isOfficialInspectionOrder(order) ? "质检仓" : "",
                order.getAddress());
    }

    private String writeShippingSnapshot(ShippingInfoDTO shippingInfo) {
        try {
            return objectMapper.writeValueAsString(shippingInfo);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist shipping snapshot");
        }
    }
}
