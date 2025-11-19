package com.xsh.trueused.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.dto.CreateOrderRequest;
import com.xsh.trueused.dto.OrderDTO;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

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
    public ResponseEntity<List<OrderDTO>> getSoldOrders(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<OrderDTO> orders = orderService.getOrdersBySeller(currentUser.getId());
        return ResponseEntity.ok(orders);
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

    @PutMapping("/{id}/ship")
    public ResponseEntity<OrderDTO> shipOrder(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrderDTO updatedOrder = orderService.shipOrder(id, currentUser.getId());
        return ResponseEntity.ok(updatedOrder);
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
}