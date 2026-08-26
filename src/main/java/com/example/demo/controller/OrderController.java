package com.example.demo.controller;

import com.example.demo.entity.Order;
import com.example.demo.entity.User;
import com.example.demo.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "${spring.web.cors.allowed-origins:http://localhost:5174}", allowCredentials = "true")
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * Fetches all successful orders for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getOrdersForUser(HttpServletRequest request) {
        try {
            User authenticatedUser = (User) request.getAttribute("authenticatedUser");
            if (authenticatedUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }

            Map<String, Object> response = orderService.getOrdersForUser(authenticatedUser);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * Customer triggers cancellation of a pending order.
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable("orderId") String orderId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        String reason = body.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cancellation reason is required"));
        }

        try {
            Order cancelledOrder = orderService.cancelOrder(user, orderId, reason);
            return ResponseEntity.ok(cancelledOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Customer triggers a return request for a delivered order.
     */
    @PostMapping("/{orderId}/return")
    public ResponseEntity<?> requestReturn(
            @PathVariable("orderId") String orderId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        String reason = body.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Return reason is required"));
        }

        try {
            Order returnedOrder = orderService.requestReturn(user, orderId, reason);
            return ResponseEntity.ok(returnedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
