package com.example.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Product;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByCategory_CategoryId(Integer categoryId);
    
    @Query("SELECT p.category.categoryName FROM Product p WHERE p.productId = :productId")
    String findCategoryNameByProductId(int productId);

    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL")
    List<String> findDistinctBrands();

    @Query("SELECT p FROM Product p WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.category.categoryName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:category IS NULL OR :category = '' OR p.category.categoryName = :category) AND " +
           "(:brand IS NULL OR :brand = '' OR p.brand = :brand) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:minRating IS NULL OR p.averageRating >= :minRating) AND " +
           "(:inStock IS NULL OR :inStock = false OR p.stock > 0)")
    Page<Product> findFilteredProducts(
            @Param("search") String search,
            @Param("category") String category,
            @Param("brand") String brand,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") Double minRating,
            @Param("inStock") Boolean inStock,
            Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.category.categoryName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Product> findSearchSuggestions(@Param("query") String query, Pageable pageable);
}
