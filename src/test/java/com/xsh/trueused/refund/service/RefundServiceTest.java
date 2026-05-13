package com.xsh.trueused.refund.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.xsh.trueused.entity.Order;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.entity.RefundRequest;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.enums.RefundStatus;
import com.xsh.trueused.enums.RefundType;
import com.xsh.trueused.notification.service.NotificationService;
import com.xsh.trueused.order.enums.OrderStatus;
import com.xsh.trueused.order.repository.OrderRepository;
import com.xsh.trueused.product.service.ProductService;
import com.xsh.trueused.refund.dto.RefundRequestCreateDTO;
import com.xsh.trueused.refund.repository.RefundRequestRepository;
import com.xsh.trueused.wallet.service.WalletService;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private WalletService walletService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private RefundService refundService;

    private Order order;

    @BeforeEach
    void setUp() {
        order = order(900L, 100L, 200L, "88.00", OrderStatus.SHIPPED);
    }

    @Test
    void requestRefundShouldMoveOrderToRefundingAndCreatePendingRequest() {
        when(orderRepository.findById(900L)).thenReturn(Optional.of(order));
        when(refundRequestRepository.findByOrderId(900L)).thenReturn(Optional.empty());

        RefundRequest refundRequest = refundService.requestRefund(900L, refundDto("50.00", RefundType.RETURN_REFUND), 100L);

        assertSame(order, refundRequest.getOrder());
        assertEquals(RefundStatus.PENDING, refundRequest.getStatus());
        assertEquals(RefundType.RETURN_REFUND, refundRequest.getRefundType());
        assertMoney("50.00", refundRequest.getRefundAmount());
        assertEquals(OrderStatus.REFUNDING, order.getStatus());
        verify(refundRequestRepository).save(refundRequest);
        verify(orderRepository).save(order);
        verify(notificationService).createNotification(eq(200L), eq("退款申请"), org.mockito.ArgumentMatchers.anyString(),
                eq("REFUND_REQUESTED"), eq(900L));
    }

    @Test
    void requestRefundShouldRejectAmountAboveOrderPrice() {
        when(orderRepository.findById(900L)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> refundService.requestRefund(900L, refundDto("100.00", RefundType.REFUND_ONLY), 100L));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Refund amount cannot exceed order amount", ex.getReason());
    }

    @Test
    void requestRefundShouldRejectDuplicatePendingRequest() {
        RefundRequest existing = refundRequest(order, RefundStatus.PENDING, RefundType.REFUND_ONLY, "30.00");
        when(orderRepository.findById(900L)).thenReturn(Optional.of(order));
        when(refundRequestRepository.findByOrderId(900L)).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> refundService.requestRefund(900L, refundDto("30.00", RefundType.REFUND_ONLY), 100L));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals("Refund request already exists", ex.getReason());
    }

    @Test
    void approveRefundOnlyShouldCompleteRefundAndRestoreProduct() {
        RefundRequest existing = refundRequest(order, RefundStatus.PENDING, RefundType.REFUND_ONLY, "88.00");
        when(orderRepository.findById(900L)).thenReturn(Optional.of(order));
        when(refundRequestRepository.findByOrderId(900L)).thenReturn(Optional.of(existing));

        RefundRequest result = refundService.approveRefund(900L, 200L);

        assertSame(existing, result);
        assertEquals(RefundStatus.COMPLETED, result.getStatus());
        assertEquals(OrderStatus.REFUNDED, order.getStatus());
        verify(walletService).refund(100L, 900L, amount("88.00"));
        verify(productService).updateProductStatus(300L, ProductStatus.ON_SALE);
    }

    @Test
    void rejectRefundShouldRestoreShippedOrderWhenTrackingExists() {
        order.setTrackingNumber("SF123");
        RefundRequest existing = refundRequest(order, RefundStatus.PENDING, RefundType.RETURN_REFUND, "20.00");
        when(orderRepository.findById(900L)).thenReturn(Optional.of(order));
        when(refundRequestRepository.findByOrderId(900L)).thenReturn(Optional.of(existing));

        RefundRequest result = refundService.rejectRefund(900L, 200L);

        assertEquals(RefundStatus.REJECTED, result.getStatus());
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        verify(orderRepository).save(order);
    }

    private static Order order(Long orderId, Long buyerId, Long sellerId, String price, OrderStatus status) {
        User buyer = new User();
        buyer.setId(buyerId);
        User seller = new User();
        seller.setId(sellerId);

        Product product = new Product();
        product.setId(300L);
        product.setSeller(seller);

        Order order = new Order();
        order.setId(orderId);
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setProduct(product);
        order.setPrice(amount(price));
        order.setStatus(status);
        return order;
    }

    private static RefundRequest refundRequest(Order order, RefundStatus status, RefundType type, String amount) {
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setOrder(order);
        refundRequest.setStatus(status);
        refundRequest.setRefundType(type);
        refundRequest.setRefundAmount(amount(amount));
        refundRequest.setReason("商品与描述不符");
        return refundRequest;
    }

    private static RefundRequestCreateDTO refundDto(String amount, RefundType type) {
        RefundRequestCreateDTO dto = new RefundRequestCreateDTO();
        dto.setReason("商品与描述不符");
        dto.setRefundType(type);
        dto.setRefundAmount(amount(amount));
        return dto;
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, amount(expected).compareTo(actual));
    }
}
