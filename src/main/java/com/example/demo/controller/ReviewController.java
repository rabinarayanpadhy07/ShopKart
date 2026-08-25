package com.example.demo.controller;

import com.example.demo.entity.Review;
import com.example.demo.entity.User;
import com.example.demo.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5174", allowCredentials = "true")
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getReviews(@PathVariable("productId") int productId) {
        List<Review> reviews = reviewService.getReviewsForProduct(productId);
        return ResponseEntity.ok(reviews);
    }

    @PostMapping
    public ResponseEntity<?> addReview(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            Integer productId = (Integer) body.get("productId");
            Integer rating = (Integer) body.get("rating");
            String comment = (String) body.get("comment");

            if (productId == null || rating == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "productId and rating are required"));
            }

            Review saved = reviewService.addReview(user, productId, rating, comment);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReview(@PathVariable("id") int reviewId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            Integer rating = (Integer) body.get("rating");
            String comment = (String) body.get("comment");

            if (rating == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "rating is required"));
            }

            Review updated = reviewService.updateReview(user, reviewId, rating, comment);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable("id") int reviewId, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            reviewService.deleteReview(user, reviewId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
