package com.example.demo.repository;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.orderId = :orderId")
    List<OrderItem> findByOrderId(String orderId);
    
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.userId = :userId AND oi.order.status NOT IN (com.example.demo.entity.OrderStatus.PENDING, com.example.demo.entity.OrderStatus.FAILED)")
    List<OrderItem> findSuccessfulOrderItemsByUserId(int userId);

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi WHERE oi.order.userId = :userId AND oi.order.status = com.example.demo.entity.OrderStatus.DELIVERED AND oi.productId = :productId")
    boolean hasUserPurchasedProduct(int userId, int productId);

}