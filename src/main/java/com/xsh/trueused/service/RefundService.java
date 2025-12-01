package com.xsh.trueused.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.xsh.trueused.dto.RefundRequestCreateDTO;
import com.xsh.trueused.entity.Order;
import com.xsh.trueused.entity.RefundRequest;
import com.xsh.trueused.enums.OrderStatus;
import com.xsh.trueused.enums.RefundStatus;
import com.xsh.trueused.repository.OrderRepository;
import com.xsh.trueused.repository.RefundRequestRepository;

@Service
public class RefundService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RefundRequestRepository refundRequestRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public RefundRequest requestRefund(Long orderId, RefundRequestCreateDTO dto, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getBuyer().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only buyer can request refund");
        }

        // 允许退款的状态：已付款、已发货、已送达（但在确认收货前？或者确认收货后一定时间内？）
        // 这里假设只要没完成或取消，且已付款就可以退款
        if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order status not eligible for refund");
        }

        // 检查是否已有退款申请
        if (refundRequestRepository.findByOrderId(orderId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Refund request already exists");
        }

        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setOrder(order);
        refundRequest.setReason(dto.getReason());
        refundRequest.setRefundType(dto.getRefundType());
        refundRequest.setRefundAmount(dto.getRefundAmount());
        refundRequest.setStatus(RefundStatus.PENDING);

        refundRequestRepository.save(refundRequest);

        // 更新订单状态
        order.setStatus(OrderStatus.REFUND_PENDING);
        orderRepository.save(order);

        // 通知卖家有退款申请
        notificationService.createNotification(
                order.getSeller().getId(),
                "退款申请",
                "订单 [" + order.getId() + "] 买家申请了退款，请及时处理。",
                "REFUND_REQUESTED",
                order.getId());

        return refundRequest;
    }

    @Transactional
    public RefundRequest approveRefund(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getSeller().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only seller can approve refund");
        }

        RefundRequest refundRequest = refundRequestRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Refund request not found"));

        if (refundRequest.getStatus() != RefundStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refund request is not pending");
        }

        refundRequest.setStatus(RefundStatus.APPROVED);
        refundRequestRepository.save(refundRequest);

        order.setStatus(OrderStatus.REFUND_APPROVED);
        orderRepository.save(order);

        // 通知买家退款已同意
        notificationService.createNotification(
                order.getBuyer().getId(),
                "退款申请已同意",
                "订单 [" + order.getId() + "] 卖家已同意退款。",
                "REFUND_APPROVED",
                order.getId());

        // TODO: Trigger actual money refund logic here if needed immediately, or wait
        // for completion

        return refundRequest;
    }

    @Transactional
    public RefundRequest rejectRefund(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getSeller().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only seller can reject refund");
        }

        RefundRequest refundRequest = refundRequestRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Refund request not found"));

        if (refundRequest.getStatus() != RefundStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refund request is not pending");
        }

        refundRequest.setStatus(RefundStatus.REJECTED);
        refundRequestRepository.save(refundRequest);

        // 恢复订单之前的状态？或者有一个专门的 REFUND_REJECTED 状态？
        // 简单起见，如果拒绝，可能需要人工介入或者恢复到 SHIPPED/DELIVERED/PAID
        // 这里暂时不改回原状态，或者需要记录原状态。
        // 为了简化，假设拒绝后订单状态变回 "已发货" 或 "已付款" 比较复杂，
        // 我们可以引入一个 REFUND_REJECTED 状态，或者让用户重新申请。
        // 这里暂时不改变 OrderStatus，或者改回之前的状态需要额外字段记录。
        // 让我们假设拒绝后，订单状态回到 SHIPPED 作为默认回退状态，或者根据是否有物流信息判断。
        if (order.getTrackingNumber() != null) {
            order.setStatus(OrderStatus.SHIPPED);
        } else {
            order.setStatus(OrderStatus.PAID);
        }
        orderRepository.save(order);

        // 通知买家退款被拒绝
        notificationService.createNotification(
                order.getBuyer().getId(),
                "退款申请被拒绝",
                "订单 [" + order.getId() + "] 卖家拒绝了您的退款申请。",
                "REFUND_REJECTED",
                order.getId());

        return refundRequest;
    }

    @Transactional
    public RefundRequest completeRefund(Long orderId) {
        // 这个方法可能由系统调用，或者卖家确认退货收到后调用
        RefundRequest refundRequest = refundRequestRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Refund request not found"));

        if (refundRequest.getStatus() != RefundStatus.APPROVED) {
            // 只有同意了才能完成
            // 或者是 RETURN_PENDING 之后的流程
        }

        refundRequest.setStatus(RefundStatus.COMPLETED);
        refundRequestRepository.save(refundRequest);

        Order order = refundRequest.getOrder();
        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);

        // 通知买家退款已完成
        notificationService.createNotification(
                order.getBuyer().getId(),
                "退款成功",
                "订单 [" + order.getId() + "] 退款流程已完成，款项将原路返回。",
                "REFUND_COMPLETED",
                order.getId());

        return refundRequest;
    }
}
