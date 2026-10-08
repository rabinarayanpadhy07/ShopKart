package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    /**
     * Fetches all successful orders for a given user and returns the required response format.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrdersForUser(User user) {
        // Fetch all successful order items for the user (non-failed, non-pending)
        List<OrderItem> orderItems = orderItemRepository.findSuccessfulOrderItemsByUserId(user.getUserId());

        // Prepare the response map
        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("role", user.getRole());

        // Transform order items into a list of product details
        List<Map<String, Object>> products = new ArrayList<>();

        if (!orderItems.isEmpty()) {
            // Batch fetch products to avoid N+1 query
            List<Integer> productIds = orderItems.stream().map(OrderItem::getProductId).distinct().toList();
            List<Product> productsList = productRepository.findAllById(productIds);
            Map<Integer, Product> productsMap = new HashMap<>();
            for (Product p : productsList) {
                productsMap.put(p.getProductId(), p);
            }

            // Batch fetch images to avoid N+1 query
            List<ProductImage> productImages = productImageRepository.findByProduct_ProductIdIn(productIds);
            Map<Integer, String> firstImageUrlMap = new HashMap<>();
            for (ProductImage img : productImages) {
                if (!firstImageUrlMap.containsKey(img.getProduct().getProductId())) {
                    firstImageUrlMap.put(img.getProduct().getProductId(), img.getImageUrl());
                }
            }

            for (OrderItem item : orderItems) {
                Product product = productsMap.get(item.getProductId());
                if (product == null) {
                    continue;
                }

                String imageUrl = firstImageUrlMap.get(product.getProductId());

                Map<String, Object> productDetails = new HashMap<>();
                productDetails.put("order_id", item.getOrder().getOrderId());
                productDetails.put("quantity", item.getQuantity());
                productDetails.put("total_price", item.getTotalPrice());
                productDetails.put("image_url", imageUrl);
                productDetails.put("product_id", product.getProductId());
                productDetails.put("name", product.getName());
                productDetails.put("description", product.getDescription());
                productDetails.put("price_per_unit", item.getPricePerUnit());
                productDetails.put("status", item.getOrder().getStatus().name());
                productDetails.put("created_at", item.getOrder().getCreatedAt().toString());
                productDetails.put("cancellation_reason", item.getOrder().getCancellationReason());
                productDetails.put("return_reason", item.getOrder().getReturnReason());

                products.add(productDetails);
            }
        }

        response.put("products", products);
        return response;
    }

    /**
     * Cancel an order if it is in an eligible status.
     * Restores stock levels atomically.
     */
    @Transactional(rollbackFor = Exception.class)
    public Order cancelOrder(User user, String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Authorize: user can cancel only their own order unless they are admin
        if (!user.getRole().name().equals("ADMIN") && order.getUserId() != user.getUserId()) {
            throw new RuntimeException("Unauthorized to cancel this order");
        }

        // Validate state transition eligibility
        OrderStatus current = order.getStatus();
        if (current == OrderStatus.CANCELLED || current == OrderStatus.DELIVERED ||
            current == OrderStatus.SHIPPED || current == OrderStatus.OUT_FOR_DELIVERY ||
            current == OrderStatus.RETURNED || current == OrderStatus.REFUNDED) {
            throw new RuntimeException("Order cannot be cancelled in its current state: " + current);
        }

        // Set cancellation audit fields
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        order.setCancellationTimestamp(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Record status history audit
        OrderStatusHistory history = new OrderStatusHistory(
                order, current, OrderStatus.CANCELLED, user.getUsername(), "Cancelled: " + reason);
        orderStatusHistoryRepository.save(history);

        // Restore inventory stock levels (batched instead of one find+save per item)
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        restoreStock(items, true);

        return order;
    }

    /**
     * Batch-restores stock for a set of order items in a single findAllById/saveAll
     * pair instead of one find+save round-trip per item.
     * @param strict if true, throws when an item's product no longer exists (matches
     *               the pre-existing cancel/CANCELLED-transition behavior); if false,
     *               silently skips missing products (matches the return/refund behavior).
     */
    private void restoreStock(List<OrderItem> items, boolean strict) {
        if (items.isEmpty()) {
            return;
        }
        List<Integer> productIds = items.stream().map(OrderItem::getProductId).distinct().toList();
        Map<Integer, Product> productsMap = new HashMap<>();
        for (Product p : productRepository.findAllById(productIds)) {
            productsMap.put(p.getProductId(), p);
        }
        List<Product> toSave = new ArrayList<>();
        for (OrderItem item : items) {
            Product product = productsMap.get(item.getProductId());
            if (product == null) {
                if (strict) {
                    throw new RuntimeException("Product not found: " + item.getProductId());
                }
                continue;
            }
            product.setStock(product.getStock() + item.getQuantity());
            toSave.add(product);
        }
        productRepository.saveAll(toSave);
    }

    /**
     * Submit a return request for a delivered order.
     */
    @Transactional(rollbackFor = Exception.class)
    public Order requestReturn(User user, String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getUserId() != user.getUserId()) {
            throw new RuntimeException("Unauthorized to request return for this order");
        }

        // Must be in DELIVERED state to request return
        OrderStatus current = order.getStatus();
        if (current != OrderStatus.DELIVERED) {
            throw new RuntimeException("Only delivered orders can be returned");
        }

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setReturnReason(reason);
        order.setReturnTimestamp(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Record status history audit
        OrderStatusHistory history = new OrderStatusHistory(
                order, current, OrderStatus.RETURN_REQUESTED, user.getUsername(), "Return Requested: " + reason);
        orderStatusHistoryRepository.save(history);

        return order;
    }

    /**
     * Admin method to fetch all orders with status/user filter.
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Admin method to fetch all orders with detailed item information.
     */
    public List<Map<String, Object>> getAllOrdersDetailed() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        // Batch fetch order items for every order, and products for every item,
        // instead of one query per order/item (was O(orders + items) DB round-trips).
        List<String> orderIds = orders.stream().map(Order::getOrderId).toList();
        List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(orderIds);

        Map<String, List<OrderItem>> itemsByOrderId = new HashMap<>();
        for (OrderItem item : allItems) {
            itemsByOrderId.computeIfAbsent(item.getOrder().getOrderId(), k -> new ArrayList<>()).add(item);
        }

        List<Integer> productIds = allItems.stream().map(OrderItem::getProductId).distinct().toList();
        Map<Integer, Product> productsMap = new HashMap<>();
        for (Product p : productRepository.findAllById(productIds)) {
            productsMap.put(p.getProductId(), p);
        }

        List<Map<String, Object>> detailedOrders = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> map = new HashMap<>();
            map.put("orderId", order.getOrderId());
            map.put("userId", order.getUserId());
            map.put("totalAmount", order.getTotalAmount());
            map.put("status", order.getStatus().name());
            map.put("createdAt", order.getCreatedAt().toString());
            map.put("formattedAddress", order.getFormattedAddress());
            map.put("cancellationReason", order.getCancellationReason());
            map.put("returnReason", order.getReturnReason());

            List<Map<String, Object>> itemMaps = new ArrayList<>();
            for (OrderItem item : itemsByOrderId.getOrDefault(order.getOrderId(), List.of())) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("productId", item.getProductId());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("pricePerUnit", item.getPricePerUnit());
                itemMap.put("totalPrice", item.getTotalPrice());

                Product p = productsMap.get(item.getProductId());
                itemMap.put("productName", p != null ? p.getName() : "Unknown Product");

                itemMaps.add(itemMap);
            }
            map.put("items", itemMaps);
            detailedOrders.add(map);
        }
        return detailedOrders;
    }

    /**
     * Update order status with valid state transitions and audit logs.
     */
    @Transactional(rollbackFor = Exception.class)
    public Order updateOrderStatus(String orderId, OrderStatus newStatus, String changedBy, String comments) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderStatus previous = order.getStatus();

        // Prevent identical transition
        if (previous == newStatus) {
            return order;
        }

        String auditComments = (comments == null || comments.trim().isEmpty())
                ? "Status transitioned to " + newStatus.name()
                : comments;

        // Validate state transition transitions rules
        validateTransition(previous, newStatus);

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        // Handle specific stock reversals or refund completions on status update
        if (newStatus == OrderStatus.CANCELLED) {
            // Restore stock if transition to cancelled
            List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
            restoreStock(items, true);
        } else if (newStatus == OrderStatus.RETURNED || newStatus == OrderStatus.REFUNDED) {
            // Optional: Restore stock when return is successfully processed/refunded
            if (previous == OrderStatus.RETURN_REQUESTED || previous == OrderStatus.RETURN_APPROVED || previous == OrderStatus.ITEM_PICKED_UP) {
                List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
                restoreStock(items, false);
            }
        }

        orderRepository.save(order);

        // Save status history audit log
        OrderStatusHistory history = new OrderStatusHistory(order, previous, newStatus, changedBy, auditComments);
        orderStatusHistoryRepository.save(history);

        return order;
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        // Hard limits on terminal status modifications
        if (current == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot modify status of a cancelled order");
        }
        if (current == OrderStatus.REFUNDED) {
            throw new RuntimeException("Cannot modify status of a refunded order");
        }

        // Restrict transitions from DELIVERED
        if (current == OrderStatus.DELIVERED) {
            if (next != OrderStatus.RETURN_REQUESTED && next != OrderStatus.RETURNED && next != OrderStatus.REFUNDED) {
                throw new RuntimeException("Delivered orders can only transition to return or refund statuses");
            }
        }
    }

    public Map<String, Object> getOrderDetailed(String orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", order.getOrderId());
        map.put("userId", order.getUserId());
        map.put("totalAmount", order.getTotalAmount());
        map.put("status", order.getStatus().name());
        map.put("createdAt", order.getCreatedAt().toString());
        map.put("formattedAddress", order.getFormattedAddress());
        map.put("cancellationReason", order.getCancellationReason());
        map.put("returnReason", order.getReturnReason());
        
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getOrderId());
        List<Integer> productIds = items.stream().map(OrderItem::getProductId).distinct().toList();
        Map<Integer, Product> productsMap = new HashMap<>();
        for (Product p : productRepository.findAllById(productIds)) {
            productsMap.put(p.getProductId(), p);
        }

        List<Map<String, Object>> itemMaps = new ArrayList<>();
        for (OrderItem item : items) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("productId", item.getProductId());
            itemMap.put("quantity", item.getQuantity());
            itemMap.put("pricePerUnit", item.getPricePerUnit());
            itemMap.put("totalPrice", item.getTotalPrice());

            Product p = productsMap.get(item.getProductId());
            itemMap.put("productName", p != null ? p.getName() : "Unknown Product");

            itemMaps.add(itemMap);
        }
        map.put("items", itemMaps);
        return map;
    }

    public List<OrderStatusHistory> getStatusHistory(String orderId) {
        return orderStatusHistoryRepository.findByOrderId(orderId);
    }
}
