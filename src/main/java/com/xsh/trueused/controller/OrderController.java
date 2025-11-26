package com.xsh.trueused.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.dto.CreateOrderRequest;
import com.xsh.trueused.dto.OrderDTO;
import com.xsh.trueused.dto.ShipOrderRequest;
import com.xsh.trueused.dto.ShippingInfoDTO;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.service.OrderService;
import com.xsh.trueused.service.ShippingService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ShippingService shippingService;

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest createOrderRequest,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrderDTO order = orderService.createOrder(createOrderRequest, currentUser.getId());
        return ResponseEntity.ok(order);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderDTO>> getMyOrders(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<OrderDTO> orders = orderService.getOrdersByBuyer(currentUser.getId());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/sold-orders")
    public Page<OrderDTO> getSoldOrders(
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String buyerName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderService.getOrdersBySeller(currentUser.getId(), productName, orderId, buyerName, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        OrderDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<OrderDTO> payOrder(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrderDTO updatedOrder = orderService.payOrder(id, currentUser.getId());
        return ResponseEntity.ok(updatedOrder);
    }

    /**
     * 发货接口 - 支持传入快递公司和单号
     */
    @PutMapping("/{id}/ship")
    public ResponseEntity<OrderDTO> shipOrder(@PathVariable Long id,
            @RequestBody(required = false) ShipOrderRequest shipRequest,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrderDTO updatedOrder = orderService.shipOrder(id, currentUser.getId(), shipRequest);
        return ResponseEntity.ok(updatedOrder);
    }

    /**
     * 获取订单物流追踪信息
     */
    @GetMapping("/{id}/shipping")
    public ResponseEntity<ShippingInfoDTO> getOrderShipping(@PathVariable Long id) {
        ShippingInfoDTO shippingInfo = orderService.getOrderShippingInfo(id);
        return ResponseEntity.ok(shippingInfo);
    }

    /**
     * 获取支持的快递公司列表
     */
    @GetMapping("/express-companies")
    public ResponseEntity<List<String>> getExpressCompanies() {
        return ResponseEntity.ok(shippingService.getSupportedExpressCompanies());
    }

    @PutMapping("/{id}/confirm-delivery")
    public ResponseEntity<OrderDTO> confirmOrderDelivery(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrderDTO updatedOrder = orderService.confirmOrderDelivery(id, currentUser.getId());
        return ResponseEntity.ok(updatedOrder);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrderDTO updatedOrder = orderService.cancelOrder(id, currentUser.getId());
        return ResponseEntity.ok(updatedOrder);
    }

    @PutMapping("/{id}/refund")
    public ResponseEntity<OrderDTO> refundOrder(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrderDTO updatedOrder = orderService.refundOrder(id, currentUser.getId());
        return ResponseEntity.ok(updatedOrder);
    }
}
