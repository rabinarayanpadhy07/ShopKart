package com.example.demo.service;

import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductImage;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.CategoryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Product> getProductsByCategory(String categoryName) {
        if (categoryName != null && !categoryName.isEmpty()) {
            Optional<Category> categoryOpt = categoryRepository.findByCategoryName(categoryName);
            if (categoryOpt.isPresent()) {
                Category category = categoryOpt.get();
                return productRepository.findByCategory_CategoryId(category.getCategoryId());
            } else {
                throw new RuntimeException("Category not found");
            }
        } else {
            return productRepository.findAll();
        }
    }

    public Page<Product> getFilteredProducts(
            String search,
            String category,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minRating,
            Boolean inStock,
            Pageable pageable) {
        return productRepository.findFilteredProducts(
                search, category, brand, minPrice, maxPrice, minRating, inStock, pageable);
    }

    public List<Product> getSearchSuggestions(String query, int limit) {
        if (query == null || query.trim().length() < 1) {
            return List.of();
        }
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, Math.max(1, limit));
        return productRepository.findSearchSuggestions(query.trim(), pageable);
    }

    public List<String> getDistinctBrands() {
        return productRepository.findDistinctBrands();
    }

    public List<String> getProductImages(Integer productId) {
        List<ProductImage> productImages = productImageRepository.findByProduct_ProductId(productId);
        List<String> imageUrls = new ArrayList<>();
        for (ProductImage image : productImages) {
            imageUrls.add(image.getImageUrl());
        }
        return imageUrls;
    }

    public java.util.Map<Integer, List<String>> getProductImagesForProducts(List<Integer> productIds) {
        List<ProductImage> productImages = productImageRepository.findByProduct_ProductIdIn(productIds);
        java.util.Map<Integer, List<String>> imagesMap = new java.util.HashMap<>();
        for (ProductImage image : productImages) {
            imagesMap.computeIfAbsent(image.getProduct().getProductId(), k -> new ArrayList<>())
                     .add(image.getImageUrl());
        }
        return imagesMap;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
}