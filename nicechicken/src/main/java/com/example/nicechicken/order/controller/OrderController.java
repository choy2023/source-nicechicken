package com.example.nicechicken.order.controller;

import com.example.nicechicken.order.dto.OrderCreateRequest;
import com.example.nicechicken.order.dto.UserOrderResponse;
import com.example.nicechicken.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.springframework.http.HttpStatus;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor // Automatic constructor generation for Dependency Injection (DI)
public class OrderController {

    private final OrderService orderService;

    // 1. Get My Orders (Authenticated)
    @GetMapping("/my")
    public ResponseEntity<List<UserOrderResponse>> getMyOrders(Authentication authentication) {
        String email = authentication.getName(); // Email extracted from JWT
        return ResponseEntity.ok(orderService.getMyOrders(email));
    }

    // 2. Cancel Order (Authenticated)
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable UUID orderId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized if user is not
                                                                           // authenticated
        }
        String email = authentication.getName();
        orderService.cancelOrder(orderId, email);
        return ResponseEntity.ok("Order has been cancelled successfully.");
    }

    // 3. Create Order (Authenticated)
    @PostMapping
    public ResponseEntity<Map<String, UUID>> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            Authentication authentication) {

        String email = (authentication != null) ? authentication.getName() : null;

        // 1. Delegate the order to the Service and receive the validated order ID
        // (UUID).
        UUID orderId = orderService.createOrder(request, email);

        // 2. Wrap the result in a response object and return.
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("orderId", orderId));
    }

    // 4. Hide Order (Authenticated)
    @PatchMapping("/{orderId}/hide")
    public ResponseEntity<String> hideOrder(@PathVariable UUID orderId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = authentication.getName();
        orderService.hideOrder(orderId, email);

        return ResponseEntity.ok("Order has been hidden successfully.");
    }
}