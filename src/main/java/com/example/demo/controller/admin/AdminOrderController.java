package com.example.demo.controller.admin;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.OrderStatusHistory;
import com.example.demo.entity.User;
import com.example.demo.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "${spring.web.cors.allowed-origins:http://localhost:5174}", allowCredentials = "true")
@RequestMapping("/admin/orders")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    /**
     * View all customer orders.
     */
    @GetMapping
    public ResponseEntity<?> getAllOrders(HttpServletRequest request) {
        User adminUser = (User) request.getAttribute("authenticatedUser");
        if (adminUser == null || !adminUser.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied. Admin role required."));
        }
        List<Map<String, Object>> orders = orderService.getAllOrdersDetailed();
        return ResponseEntity.ok(orders);
    }

    /**
     * Get order status transition audit history log.
     */
    @GetMapping("/{orderId}/history")
    public ResponseEntity<?> getOrderStatusHistory(
            @PathVariable("orderId") String orderId,
            HttpServletRequest request) {
        User adminUser = (User) request.getAttribute("authenticatedUser");
        if (adminUser == null || !adminUser.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied. Admin role required."));
        }
        List<OrderStatusHistory> history = orderService.getStatusHistory(orderId);
        return ResponseEntity.ok(history);
    }

    /**
     * Update order status.
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable("orderId") String orderId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        User adminUser = (User) request.getAttribute("authenticatedUser");
        if (adminUser == null || !adminUser.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied. Admin role required."));
        }

        String statusStr = body.get("status");
        String comments = body.get("comments");

        if (statusStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
        }

        try {
            OrderStatus newStatus = OrderStatus.valueOf(statusStr.toUpperCase());
            orderService.updateOrderStatus(orderId, newStatus, adminUser.getUsername(), comments);
            Map<String, Object> updatedOrderDetailed = orderService.getOrderDetailed(orderId);
            return ResponseEntity.ok(updatedOrderDetailed);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
