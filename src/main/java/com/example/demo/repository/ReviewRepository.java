package com.example.demo.repository;

import com.example.demo.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    @Query("SELECT r FROM Review r WHERE r.product.productId = :productId ORDER BY r.createdAt DESC")
    List<Review> findByProductId(int productId);

    @Query("SELECT r FROM Review r WHERE r.user.userId = :userId AND r.product.productId = :productId")
    Optional<Review> findByUserIdAndProductId(int userId, int productId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.product.productId = :productId")
    Double getAverageRatingForProduct(int productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.productId = :productId")
    Integer getReviewCountForProduct(int productId);
}
