package com.example.demo.service;

import com.example.demo.entity.Product;
import com.example.demo.entity.ProductImage;
import com.example.demo.entity.User;
import com.example.demo.entity.WishlistItem;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Service
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private CartService cartService;

    public List<WishlistItem> getWishlistForUser(User user) {
        return wishlistRepository.findByUserId(user.getUserId());
    }

    public List<Map<String, Object>> getDetailedWishlistForUser(User user) {
        List<WishlistItem> wishlist = wishlistRepository.findByUserId(user.getUserId());
        List<Map<String, Object>> detailedWishlist = new ArrayList<>();
        for (WishlistItem item : wishlist) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            
            // Map product details
            Product product = item.getProduct();
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("productId", product.getProductId());
            productMap.put("name", product.getName());
            productMap.put("description", product.getDescription());
            productMap.put("price", product.getPrice());
            productMap.put("stock", product.getStock());
            productMap.put("brand", product.getBrand());
            
            // Fetch product image
            List<ProductImage> productImages = productImageRepository.findByProduct_ProductId(product.getProductId());
            if (productImages != null && !productImages.isEmpty()) {
                productMap.put("imageUrl", productImages.get(0).getImageUrl());
            } else {
                productMap.put("imageUrl", null);
            }
            map.put("product", productMap);
            
            // Map user details
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("userId", user.getUserId());
            userMap.put("username", user.getUsername());
            map.put("user", userMap);
            
            detailedWishlist.add(map);
        }
        return detailedWishlist;
    }

    @Transactional
    public WishlistItem addToWishlist(User user, int productId) {
        Optional<WishlistItem> existing = wishlistRepository.findByUserIdAndProductId(user.getUserId(), productId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        WishlistItem wishlistItem = new WishlistItem();
        wishlistItem.setUser(user);
        wishlistItem.setProduct(product);
        wishlistItem.setCreatedAt(LocalDateTime.now());

        return wishlistRepository.save(wishlistItem);
    }

    @Transactional
    public void removeFromWishlist(User user, int productId) {
        WishlistItem wishlistItem = wishlistRepository.findByUserIdAndProductId(user.getUserId(), productId)
                .orElseThrow(() -> new RuntimeException("Wishlist item not found"));
        wishlistRepository.delete(wishlistItem);
    }

    @Transactional
    public void moveWishlistItemToCart(User user, int productId) {
        // Add to cart
        cartService.addToCart(user.getUserId(), productId, 1);
        // Remove from wishlist if exists
        wishlistRepository.findByUserIdAndProductId(user.getUserId(), productId)
                .ifPresent(wishlistItem -> wishlistRepository.delete(wishlistItem));
    }
}
