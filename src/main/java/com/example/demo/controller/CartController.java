package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.CartService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "${spring.web.cors.allowed-origins:http://localhost:5174}", allowCredentials = "true")
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    // Fetch user cart item count using authenticated context
    @GetMapping("/items/count")
    public ResponseEntity<Integer> getCartItemCount(HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        int count = cartService.getCartItemCount(user.getUserId());
        return ResponseEntity.ok(count);
    }

    // Fetch all cart items for the authenticated user
    @GetMapping("/items")
    public ResponseEntity<Map<String, Object>> getCartItems(HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Map<String, Object> cartItems = cartService.getCartItems(user.getUserId());
        return ResponseEntity.ok(cartItems);
    }

    // Add an item to the cart for authenticated user
    @PostMapping("/add")
    public ResponseEntity<Void> addToCart(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        int productId = readPositiveInt(requestBody, "productId");
        int quantity = requestBody.containsKey("quantity") ? readPositiveInt(requestBody, "quantity") : 1;

        cartService.addToCart(user.getUserId(), productId, quantity);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Update Cart Item Quantity for authenticated user
    @PutMapping("/update")
    public ResponseEntity<Void> updateCartItemQuantity(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        int productId = readPositiveInt(requestBody, "productId");
        int quantity = readNonNegativeInt(requestBody, "quantity");

        cartService.updateCartItemQuantity(user.getUserId(), productId, quantity);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // Delete Cart Item for authenticated user
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteCartItem(@RequestParam("productId") int productId, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        cartService.deleteCartItem(user.getUserId(), productId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    private int readPositiveInt(Map<String, Object> requestBody, String field) {
        int value = readInt(requestBody, field);
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be greater than 0");
        }
        return value;
    }

    private int readNonNegativeInt(Map<String, Object> requestBody, String field) {
        int value = readInt(requestBody, field);
        if (value < 0) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
        return value;
    }

    private int readInt(Map<String, Object> requestBody, String field) {
        Object raw = requestBody.get(field);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String text) {
            return Integer.parseInt(text);
        }
        throw new IllegalArgumentException(field + " is required");
    }
}
