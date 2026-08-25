package com.example.demo.repository;

import com.example.demo.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Integer> {
    @Query("SELECT w FROM WishlistItem w WHERE w.user.userId = :userId ORDER BY w.createdAt DESC")
    List<WishlistItem> findByUserId(int userId);

    @Query("SELECT w FROM WishlistItem w WHERE w.user.userId = :userId AND w.product.productId = :productId")
    Optional<WishlistItem> findByUserIdAndProductId(int userId, int productId);
}
