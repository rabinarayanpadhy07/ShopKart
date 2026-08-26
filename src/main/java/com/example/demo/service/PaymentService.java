package com.example.demo.service;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.Product;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public PaymentService(OrderRepository orderRepository, 
                          OrderItemRepository orderItemRepository, 
                          CartRepository cartRepository,
                          ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public String createOrder(int userId, BigDecimal totalAmount, List<OrderItem> orderItems, Integer addressId, String formattedAddress) throws RazorpayException {
        // Create Razorpay client
        RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        // Prepare Razorpay order request
        var orderRequest = new JSONObject();
        orderRequest.put("amount", totalAmount.multiply(BigDecimal.valueOf(100)).intValue()); // Amount in paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

        // Create Razorpay order
        com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        // Save order details in the database
        Order order = new Order();
        order.setOrderId(razorpayOrder.get("id"));
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setAddressId(addressId);
        order.setFormattedAddress(formattedAddress);
        orderRepository.save(order);

        for (OrderItem item : orderItems) {
            item.setOrder(order);
            orderItemRepository.save(item);
        }

        return razorpayOrder.get("id");
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature, int userId) {
        // Prepare signature validation attributes
        JSONObject attributes = new JSONObject();
        attributes.put("razorpay_order_id", razorpayOrderId);
        attributes.put("razorpay_payment_id", razorpayPaymentId);
        attributes.put("razorpay_signature", razorpaySignature);

        boolean isSignatureValid;
        try {
            isSignatureValid = com.razorpay.Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
        } catch (Exception e) {
            throw new RuntimeException("Payment signature verification failed", e);
        }

        if (!isSignatureValid) {
            return false;
        }

        Order order = orderRepository.findById(razorpayOrderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getUserId() != userId) {
            throw new RuntimeException("Payment order does not belong to this user");
        }
        if (order.getStatus() == OrderStatus.SUCCESS) {
            return true;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order is not payable in its current state: " + order.getStatus());
        }

        order.setStatus(OrderStatus.SUCCESS);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(razorpayOrderId);
        for (OrderItem orderItem : orderItems) {
            Product product = productRepository.findById(orderItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (product.getStock() < orderItem.getQuantity()) {
                throw new RuntimeException("Stock is insufficient for product: " + product.getName());
            }

            product.setStock(product.getStock() - orderItem.getQuantity());
            productRepository.save(product);
        }

        cartRepository.deleteAllCartItemsByUserId(userId);

        return true;
    }

    @Transactional
    public void saveOrderItems(String orderId, List<OrderItem> items) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        for (OrderItem item : items) {
            item.setOrder(order);
            orderItemRepository.save(item);
        }
    }
}
