package com.example.demo.controller;

import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5174", allowCredentials = "true")
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(defaultValue = "productId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            HttpServletRequest request) {
        try {
            User authenticatedUser = (User) request.getAttribute("authenticatedUser");

            Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            PageRequest pageable = PageRequest.of(page, size, sort);

            // Fetch page of products based on query and filters
            Page<Product> productPage = productService.getFilteredProducts(
                    search, category, brand, minPrice, maxPrice, minRating, inStock, pageable);

            List<Product> products = productPage.getContent();

            // Build response
            Map<String, Object> response = new HashMap<>();

            // User Info
            Map<String, String> userInfo = new HashMap<>();
            if (authenticatedUser != null) {
                userInfo.put("name", authenticatedUser.getUsername());
                userInfo.put("role", authenticatedUser.getRole().name());
            } else {
                userInfo.put("name", "Guest");
                userInfo.put("role", "GUEST");
            }
            response.put("user", userInfo);

            // Batch fetch product images
            List<Integer> productIds = products.stream().map(Product::getProductId).toList();
            java.util.Map<Integer, List<String>> imagesMap = productService.getProductImagesForProducts(productIds);

            // Build product details maps
            List<Map<String, Object>> productList = new ArrayList<>();
            for (Product product : products) {
                Map<String, Object> productDetails = new HashMap<>();
                productDetails.put("product_id", product.getProductId());
                productDetails.put("name", product.getName());
                productDetails.put("description", product.getDescription());
                productDetails.put("price", product.getPrice());
                productDetails.put("stock", product.getStock());
                productDetails.put("brand", product.getBrand());
                productDetails.put("category", product.getCategory() != null ? product.getCategory().getCategoryName() : "");
                productDetails.put("averageRating", product.getAverageRating());
                productDetails.put("totalReviews", product.getTotalReviews());

                List<String> images = imagesMap.getOrDefault(product.getProductId(), List.of());
                productDetails.put("images", images);

                productList.add(productDetails);
            }

            response.put("products", productList);
            response.put("currentPage", productPage.getNumber());
            response.put("totalItems", productPage.getTotalElements());
            response.put("totalPages", productPage.getTotalPages());
            response.put("pageSize", productPage.getSize());
            
            // Send list of all brands so the UI can construct filter checklist
            response.put("brands", productService.getDistinctBrands());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/suggestions")
    public ResponseEntity<Map<String, Object>> getSuggestions(@RequestParam("q") String query) {
        try {
            List<Product> matches = productService.getSearchSuggestions(query, 8);
            if (matches.isEmpty()) {
                return ResponseEntity.ok(Map.of("suggestions", List.of()));
            }
            List<Integer> productIds = matches.stream().map(Product::getProductId).toList();
            Map<Integer, List<String>> imagesMap = productService.getProductImagesForProducts(productIds);

            List<Map<String, Object>> suggestions = new ArrayList<>();
            for (Product product : matches) {
                Map<String, Object> item = new HashMap<>();
                item.put("product_id", product.getProductId());
                item.put("name", product.getName());
                item.put("description", product.getDescription());
                item.put("price", product.getPrice());
                item.put("brand", product.getBrand());
                item.put("category", product.getCategory() != null ? product.getCategory().getCategoryName() : "");
                List<String> images = imagesMap.getOrDefault(product.getProductId(), List.of());
                item.put("image", images.isEmpty() ? null : images.get(0));
                suggestions.add(item);
            }
            return ResponseEntity.ok(Map.of("suggestions", suggestions));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        try {
            return ResponseEntity.ok(productService.getAllCategories());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
