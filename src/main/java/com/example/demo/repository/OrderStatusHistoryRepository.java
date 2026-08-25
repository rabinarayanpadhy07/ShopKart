package com.example.demo.repository;

import com.example.demo.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Integer> {
    @Query("SELECT h FROM OrderStatusHistory h WHERE h.order.orderId = :orderId ORDER BY h.timestamp DESC")
    List<OrderStatusHistory> findByOrderId(String orderId);
}
