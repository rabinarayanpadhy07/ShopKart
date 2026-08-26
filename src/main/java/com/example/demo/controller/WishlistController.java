package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.entity.WishlistItem;
import com.example.demo.service.WishlistService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "${spring.web.cors.allowed-origins:http://localhost:5174}", allowCredentials = "true")
@RequestMapping("/api/wishlist")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<?> getWishlist(HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        List<Map<String, Object>> wishlist = wishlistService.getDetailedWishlistForUser(user);
        return ResponseEntity.ok(wishlist);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToWishlist(@RequestBody Map<String, Integer> body, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            Integer productId = body.get("productId");
            if (productId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "productId is required"));
            }
            WishlistItem item = wishlistService.addToWishlist(user, productId);
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<?> removeFromWishlist(@PathVariable("productId") int productId, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            wishlistService.removeFromWishlist(user, productId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/move-to-cart")
    public ResponseEntity<?> moveWishlistItemToCart(@RequestBody Map<String, Integer> body, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            Integer productId = body.get("productId");
            if (productId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "productId is required"));
            }
            wishlistService.moveWishlistItemToCart(user, productId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
