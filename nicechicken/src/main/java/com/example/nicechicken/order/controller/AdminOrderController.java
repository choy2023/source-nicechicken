package com.example.nicechicken.order.controller;

import com.example.nicechicken.order.dto.AdminOrderResponse;
import com.example.nicechicken.order.dto.OrderStatusHistoryResponse;
import com.example.nicechicken.order.entity.OrderStatus;
import com.example.nicechicken.order.service.OrderService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<AdminOrderResponse>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID orderId,
            @RequestBody Map<String, String> statusRequest) {
        
        OrderStatus newStatus = OrderStatus.valueOf(statusRequest.get("status"));
        orderService.updateOrderStatus(orderId, newStatus);
        
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{orderId}/history")
    public ResponseEntity<List<OrderStatusHistoryResponse>> getOrderHistory(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrderHistory(orderId));
    }
}