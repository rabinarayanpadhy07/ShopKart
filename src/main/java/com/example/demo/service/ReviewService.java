package com.example.demo.service;

import com.example.demo.entity.Product;
import com.example.demo.entity.Review;
import com.example.demo.entity.User;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    public List<Review> getReviewsForProduct(int productId) {
        return reviewRepository.findByProductId(productId);
    }

    @Transactional
    public Review addReview(User user, int productId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        // Check if user purchased the product
        boolean purchased = orderItemRepository.hasUserPurchasedProduct(user.getUserId(), productId);
        if (!purchased) {
            throw new RuntimeException("Reviews are restricted to customers who purchased this product");
        }

        // Check if user already reviewed the product
        Optional<Review> existing = reviewRepository.findByUserIdAndProductId(user.getUserId(), productId);
        if (existing.isPresent()) {
            throw new RuntimeException("You have already submitted a review for this product");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        Review saved = reviewRepository.save(review);

        // Recalculate average rating & total reviews
        updateProductRatingStats(product);

        return saved;
    }

    @Transactional
    public Review updateReview(User user, int reviewId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!review.getUser().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Unauthorized to update this review");
        }

        review.setRating(rating);
        review.setComment(comment);
        review.setUpdatedAt(LocalDateTime.now());

        Review saved = reviewRepository.save(review);

        // Recalculate stats
        updateProductRatingStats(review.getProduct());

        return saved;
    }

    @Transactional
    public void deleteReview(User user, int reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // Allow owner OR an admin to delete
        if (!review.getUser().getUserId().equals(user.getUserId()) && !user.getRole().name().equals("ADMIN")) {
            throw new RuntimeException("Unauthorized to delete this review");
        }

        Product product = review.getProduct();
        reviewRepository.delete(review);

        // Recalculate stats
        updateProductRatingStats(product);
    }

    private void updateProductRatingStats(Product product) {
        Double avgRating = reviewRepository.getAverageRatingForProduct(product.getProductId());
        Integer count = reviewRepository.getReviewCountForProduct(product.getProductId());
        product.setAverageRating(avgRating != null ? avgRating : 0.0);
        product.setTotalReviews(count != null ? count : 0);
        productRepository.save(product);
    }
}
