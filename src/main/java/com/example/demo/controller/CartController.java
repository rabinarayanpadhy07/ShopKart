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
@CrossOrigin(origins = "http://localhost:5174", allowCredentials = "true")
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
        
        int productId = (int) requestBody.get("productId");
        int quantity = requestBody.containsKey("quantity") ? (int) requestBody.get("quantity") : 1;

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

        int productId = (int) requestBody.get("productId");
        int quantity = (int) requestBody.get("quantity");

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
}
