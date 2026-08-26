package com.example.demo.controller;

import com.example.demo.entity.Address;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.User;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.AddressRepository;
import com.example.demo.service.PaymentService;
import com.razorpay.RazorpayException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "${spring.web.cors.allowed-origins:http://localhost:5174}", allowCredentials = "true")
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private AddressRepository addressRepository;

    /**
     * Create Razorpay Order
     * Calculated on the server based on database cart items to prevent tampering.
     * Optionally accepts addressId in request body.
     */
    @PostMapping("/create")
    public ResponseEntity<String> createPaymentOrder(@RequestBody(required = false) Map<String, Integer> requestBody, HttpServletRequest request) {
        try {
            // Fetch authenticated user
            User user = (User) request.getAttribute("authenticatedUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            // Address selection logic
            Integer addressId = requestBody != null ? requestBody.get("addressId") : null;
            String formattedAddress = null;
            if (addressId != null) {
                Address address = addressRepository.findById(addressId)
                        .orElseThrow(() -> new RuntimeException("Address not found"));
                if (!address.getUser().getUserId().equals(user.getUserId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied to this address");
                }
                formattedAddress = String.format("%s, %s, %s, %s, State: %s - %s",
                        address.getFullName(), address.getPhoneNumber(), address.getStreetAddress(),
                        address.getCity(), address.getState(), address.getZipCode());
            } else {
                // Try to find default address for the user
                List<Address> addresses = addressRepository.findByUserId(user.getUserId());
                if (!addresses.isEmpty()) {
                    Address address = addresses.stream().filter(Address::isDefault).findFirst().orElse(addresses.get(0));
                    addressId = address.getId();
                    formattedAddress = String.format("%s, %s, %s, %s, State: %s - %s",
                            address.getFullName(), address.getPhoneNumber(), address.getStreetAddress(),
                            address.getCity(), address.getState(), address.getZipCode());
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please add a shipping address first");
                }
            }

            // Load user's cart items from the database
            List<CartItem> cartItems = cartRepository.findCartItemsWithProductDetails(user.getUserId());
            if (cartItems.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot checkout: Cart is empty");
            }

            // Server-side calculation of the total amount and verification of stock
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (CartItem item : cartItems) {
                if (item.getProduct().getStock() < item.getQuantity()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Insufficient stock for product: " + item.getProduct().getName());
                }
                BigDecimal itemTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                totalAmount = totalAmount.add(itemTotal);
            }

            // Add shipping charge (₹370) if totalAmount is less than 499
            if (totalAmount.compareTo(BigDecimal.valueOf(499)) < 0) {
                totalAmount = totalAmount.add(BigDecimal.valueOf(370));
            }

            // Convert cart items to OrderItem entities
            List<OrderItem> orderItems = cartItems.stream().map(item -> {
                OrderItem orderItem = new OrderItem();
                orderItem.setProductId(item.getProduct().getProductId());
                orderItem.setQuantity(item.getQuantity());
                BigDecimal pricePerUnit = item.getProduct().getPrice();
                orderItem.setPricePerUnit(pricePerUnit);
                orderItem.setTotalPrice(pricePerUnit.multiply(BigDecimal.valueOf(item.getQuantity())));
                return orderItem;
            }).collect(Collectors.toList());

            // Call the payment service to create a Razorpay order
            String razorpayOrderId = paymentService.createOrder(user.getUserId(), totalAmount, orderItems, addressId, formattedAddress);

            return ResponseEntity.ok(razorpayOrderId);
        } catch (RazorpayException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating Razorpay order: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request data: " + e.getMessage());
        }
    }

    /**
     * Verify Razorpay Payment
     */
    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        try {
            // Fetch authenticated user
            User user = (User) request.getAttribute("authenticatedUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            int userId = user.getUserId();
            
            // Extract Razorpay payment details
            String razorpayOrderId = (String) requestBody.get("razorpayOrderId");
            String razorpayPaymentId = (String) requestBody.get("razorpayPaymentId");
            String razorpaySignature = (String) requestBody.get("razorpaySignature");

            // Call the payment service to verify the payment
            boolean isVerified = paymentService.verifyPayment(razorpayOrderId, razorpayPaymentId, razorpaySignature, userId);

            if (isVerified) {
                return ResponseEntity.ok("Payment verified successfully");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment verification failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error verifying payment: " + e.getMessage());
        }
    }
}
