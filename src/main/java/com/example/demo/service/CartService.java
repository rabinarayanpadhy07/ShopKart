package com.example.demo.service;

import com.example.demo.entity.CartItem;
import com.example.demo.entity.User;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductImage;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CartService {

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductImageRepository productImageRepository;

	// Get the total cart item count for a user
	public int getCartItemCount(int userId) {
		return cartRepository.countTotalItems(userId);
	}

	// Add an item to the cart
	public void addToCart(int userId, int productId, int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be greater than 0");
		}

		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

		// Fetch cart item for this userId and productId
		Optional<CartItem> existingItem = cartRepository.findByUserAndProduct(userId, productId);

		if (existingItem.isPresent()) {
			CartItem cartItem = existingItem.get();
			int newQuantity = cartItem.getQuantity() + quantity;
			validateAvailableStock(product, newQuantity);
			cartItem.setQuantity(newQuantity);
			cartRepository.save(cartItem);
		} else {
			validateAvailableStock(product, quantity);
			// Use reference proxy to eliminate unnecessary user database select
			User userRef = userRepository.getReferenceById(userId);
			CartItem newItem = new CartItem(userRef, product, quantity);
			cartRepository.save(newItem);
		}
	}

	// Get Cart Items for a User (with known username/role to avoid redundant user DB query)
	public Map<String, Object> getCartItems(int userId, String username, String role) {
		List<CartItem> cartItems = cartRepository.findCartItemsWithProductDetails(userId);

		Map<String, Object> response = new HashMap<>();
		response.put("username", username);
		response.put("role", role);

		List<Map<String, Object>> products = new ArrayList<>();
		double overallTotalPrice = 0;

		// Batch fetch all product images in a single query to eliminate N+1
		List<Integer> productIds = cartItems.stream()
				.map(item -> item.getProduct().getProductId())
				.distinct()
				.toList();

		Map<Integer, String> firstImageMap = new HashMap<>();
		if (!productIds.isEmpty()) {
			List<ProductImage> productImages = productImageRepository.findByProduct_ProductIdIn(productIds);
			for (ProductImage image : productImages) {
				if (image != null && image.getProduct() != null) {
					firstImageMap.putIfAbsent(image.getProduct().getProductId(), image.getImageUrl());
				}
			}
		}

		for (CartItem cartItem : cartItems) {
			Map<String, Object> productDetails = new HashMap<>();
			Product product = cartItem.getProduct();

			String imageUrl = firstImageMap.getOrDefault(product.getProductId(), "default-image-url");

			productDetails.put("product_id", product.getProductId());
			productDetails.put("image_url", imageUrl);
			productDetails.put("name", product.getName());
			productDetails.put("description", product.getDescription());
			productDetails.put("price_per_unit", product.getPrice());
			productDetails.put("quantity", cartItem.getQuantity());
			productDetails.put("total_price", cartItem.getQuantity() * product.getPrice().doubleValue());

			products.add(productDetails);
			overallTotalPrice += cartItem.getQuantity() * product.getPrice().doubleValue();
		}

		Map<String, Object> cart = new HashMap<>();
		cart.put("products", products);
		cart.put("overall_total_price", overallTotalPrice);

		response.put("cart", cart);
		return response;
	}

	// Get Cart Items for a User (fallback if username/role unknown)
	public Map<String, Object> getCartItems(int userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
		return getCartItems(userId, user.getUsername(), user.getRole().toString());
	}

	// Update Cart Item Quantity
	public void updateCartItemQuantity(int userId, int productId, int quantity) {
		if (quantity < 0) {
			throw new IllegalArgumentException("Quantity cannot be negative");
		}

		// Fetch cart item for this userId and productId
		Optional<CartItem> existingItem = cartRepository.findByUserAndProduct(userId, productId);

		if (existingItem.isPresent()) {
			CartItem cartItem = existingItem.get();
			if (quantity == 0) {
				deleteCartItem(userId, productId);
			} else {
				validateAvailableStock(cartItem.getProduct(), quantity);
				cartItem.setQuantity(quantity);
				cartRepository.save(cartItem);
			}
		} else if (quantity > 0) {
			Product product = productRepository.findById(productId)
					.orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
			validateAvailableStock(product, quantity);
			User userRef = userRepository.getReferenceById(userId);
			CartItem newItem = new CartItem(userRef, product, quantity);
			cartRepository.save(newItem);
		}
	}

	private void validateAvailableStock(Product product, int quantity) {
		if (quantity > product.getStock()) {
			throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
		}
	}

	// Delete Cart Item
	public void deleteCartItem(int userId, int productId) {
		cartRepository.deleteCartItem(userId, productId);
	}
}
