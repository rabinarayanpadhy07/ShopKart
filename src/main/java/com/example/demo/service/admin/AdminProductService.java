package com.example.demo.service.admin;

import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductImage;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AdminProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;

    public AdminProductService(ProductRepository productRepository, ProductImageRepository productImageRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
    }

    public Product addProductWithImage(String name, String description, Double price, Integer stock, Integer categoryId, String imageUrl, String brand) {
        // Validate the category
        Optional<Category> category = categoryRepository.findById(categoryId);
        if (category.isEmpty()) {
            throw new IllegalArgumentException("Invalid category ID");
        }

        // Create and save the product
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setBrand(brand != null && !brand.isBlank() ? brand.trim() : null);
        product.setPrice(BigDecimal.valueOf(price));
        product.setStock(stock);
        product.setCategory(category.get());
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);

        // Create and save the product image
        if (imageUrl != null && !imageUrl.isEmpty()) {
            ProductImage productImage = new ProductImage();
            productImage.setProduct(savedProduct);
            productImage.setImageUrl(imageUrl);
            productImageRepository.save(productImage);
        } else {
            throw new IllegalArgumentException("Product image URL cannot be empty");
        }

        return savedProduct;
    }
    
    public void deleteProduct(Integer productId) {
        // Check if the product exists
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("Product not found");
        }

        // Delete associated product images
        productImageRepository.deleteByProductId(productId);

        // Delete the product
        productRepository.deleteById(productId);
    }

    public Product modifyProduct(Integer productId, String name, String description, Double price, Integer stock, Integer categoryId, String imageUrl, String brand) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));
            product.setCategory(category);
        }

        if (name != null) product.setName(name);
        if (description != null) product.setDescription(description);
        if (price != null) product.setPrice(BigDecimal.valueOf(price));
        if (stock != null) product.setStock(stock);
        if (brand != null) product.setBrand(brand.isBlank() ? null : brand.trim());
        product.setUpdatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);

        // Only the cover (first) image is editable from the admin form; swap it in
        // place so any additional gallery images are preserved.
        if (imageUrl != null && !imageUrl.isEmpty()) {
            List<ProductImage> existing = productImageRepository.findByProduct_ProductIdInOrderByImageIdAsc(List.of(productId));
            ProductImage cover = existing.isEmpty() ? new ProductImage() : existing.get(0);
            if (!imageUrl.equals(cover.getImageUrl())) {
                cover.setProduct(savedProduct);
                cover.setImageUrl(imageUrl);
                productImageRepository.save(cover);
            }
        }

        return savedProduct;
    }

    public Category addCategory(String categoryName) {
        if (categoryRepository.findByCategoryName(categoryName).isPresent()) {
            throw new IllegalArgumentException("Category already exists");
        }
        Category category = new Category();
        category.setCategoryName(categoryName);
        return categoryRepository.save(category);
    }
}