// Admin Controller for Add Product functionality
package com.example.demo.controller.admin;

import com.example.demo.entity.Product;
import com.example.demo.entity.Category;
import com.example.demo.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import com.example.demo.service.admin.AdminProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin/products")
public class AdminProductController {

    private final AdminProductService adminProductService;

    public AdminProductController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addProduct(@RequestBody Map<String, Object> productRequest) {
        try {
            String name = (String) productRequest.get("name");
            String description = (String) productRequest.get("description");
            Double price = Double.valueOf(String.valueOf(productRequest.get("price")));
            Integer stock = (Integer) productRequest.get("stock");
            Integer categoryId = (Integer) productRequest.get("categoryId");
            String imageUrl = (String) productRequest.get("imageUrl");

            Product addedProduct = adminProductService.addProductWithImage(name, description, price, stock, categoryId, imageUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(addedProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Something went wrong"));
        }
    }
    
    
    @DeleteMapping("/delete/{productId}")
    public ResponseEntity<?> deleteProduct(@PathVariable("productId") Integer productId) {
        try {
            adminProductService.deleteProduct(productId);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Product deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Something went wrong"));
        }
    }

    @PutMapping("/modify/{productId}")
    public ResponseEntity<?> modifyProduct(@PathVariable("productId") Integer productId, @RequestBody Map<String, Object> productRequest) {
        try {
            String name = (String) productRequest.get("name");
            String description = (String) productRequest.get("description");
            Double price = productRequest.get("price") != null ? Double.valueOf(String.valueOf(productRequest.get("price"))) : null;
            Integer stock = (Integer) productRequest.get("stock");
            Integer categoryId = (Integer) productRequest.get("categoryId");
            String imageUrl = (String) productRequest.get("imageUrl");

            Product modified = adminProductService.modifyProduct(productId, name, description, price, stock, categoryId, imageUrl);
            return ResponseEntity.status(HttpStatus.OK).body(modified);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Something went wrong"));
        }
    }

    @PostMapping("/categories/add")
    public ResponseEntity<?> addCategory(HttpServletRequest request, @RequestBody Map<String, String> payload) {
        User adminUser = (User) request.getAttribute("authenticatedUser");
        if (adminUser == null || !adminUser.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied. Admin role required."));
        }
        String name = payload.get("categoryName");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Category name cannot be empty"));
        }
        try {
            Category category = adminProductService.addCategory(name.trim());
            return ResponseEntity.status(HttpStatus.CREATED).body(category);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Something went wrong"));
        }
    }
}