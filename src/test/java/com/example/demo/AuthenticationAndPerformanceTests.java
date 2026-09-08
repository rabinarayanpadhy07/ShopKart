package com.example.demo;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AuthenticationAndPerformanceTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private jakarta.servlet.Filter springSecurityFilterChain;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private JWTTokenRepository jwtTokenRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private com.example.demo.service.RateLimitService rateLimitService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private User customerUser;
    private User adminUser;
    private User otherCustomer;
    private Product testProduct;
    private Category testCategory;
    private String customerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();

        cartRepository.deleteAll();
        orderStatusHistoryRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        reviewRepository.deleteAll();
        addressRepository.deleteAll();
        productImageRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        jwtTokenRepository.deleteAll();
        userRepository.deleteAll();
        rateLimitService.clearAll();

        // Seed Customer
        customerUser = new User();
        customerUser.setUsername("john_customer");
        customerUser.setEmail("john@example.com");
        customerUser.setPassword(passwordEncoder.encode("secret123"));
        customerUser.setRole(Role.CUSTOMER);
        customerUser.setCreatedAt(LocalDateTime.now());
        customerUser.setUpdatedAt(LocalDateTime.now());
        customerUser = userRepository.save(customerUser);

        // Seed Admin
        adminUser = new User();
        adminUser.setUsername("sarah_admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("adminSecret123"));
        adminUser.setRole(Role.ADMIN);
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser.setUpdatedAt(LocalDateTime.now());
        adminUser = userRepository.save(adminUser);

        // Seed Other Customer
        otherCustomer = new User();
        otherCustomer.setUsername("other_customer");
        otherCustomer.setEmail("other@example.com");
        otherCustomer.setPassword(passwordEncoder.encode("secret123"));
        otherCustomer.setRole(Role.CUSTOMER);
        otherCustomer.setCreatedAt(LocalDateTime.now());
        otherCustomer.setUpdatedAt(LocalDateTime.now());
        otherCustomer = userRepository.save(otherCustomer);

        // Seed Category & Product
        testCategory = new Category();
        testCategory.setCategoryName("Electronics");
        testCategory = categoryRepository.save(testCategory);

        testProduct = new Product();
        testProduct.setName("Wireless Noise-Cancelling Headphones");
        testProduct.setDescription("Premium over-ear wireless headphones with active noise cancellation");
        testProduct.setBrand("Sony");
        testProduct.setPrice(BigDecimal.valueOf(14999.00));
        testProduct.setStock(25);
        testProduct.setCategory(testCategory);
        testProduct.setAverageRating(4.5);
        testProduct.setTotalReviews(12);
        testProduct.setCreatedAt(LocalDateTime.now());
        testProduct.setUpdatedAt(LocalDateTime.now());
        testProduct = productRepository.save(testProduct);

        ProductImage img = new ProductImage();
        img.setProduct(testProduct);
        img.setImageUrl("https://images.example.com/headphones.jpg");
        productImageRepository.save(img);

        // Generate tokens
        customerToken = authService.generateToken(customerUser);
        adminToken = authService.generateToken(adminUser);
    }

    // -------------------------------------------------------------
    // 1. LOGIN TEST
    // -------------------------------------------------------------
    @Test
    @DisplayName("1. Login with valid credentials returns 200, sets authToken cookie, and provides user details")
    void testLoginSuccess() throws Exception {
        String loginPayload = """
            {
                "username": "john_customer",
                "password": "secret123"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("authToken"))
                .andExpect(cookie().httpOnly("authToken", true))
                .andExpect(jsonPath("$.message", is("Login successful")))
                .andExpect(jsonPath("$.role", is("CUSTOMER")))
                .andExpect(jsonPath("$.username", is("john_customer")));
    }

    // -------------------------------------------------------------
    // 2. WRONG PASSWORD TEST
    // -------------------------------------------------------------
    @Test
    @DisplayName("2. Login with wrong password returns 401 Unauthorized")
    void testLoginWrongPassword() throws Exception {
        String loginPayload = """
            {
                "username": "john_customer",
                "password": "wrong_password"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("Invalid username or password")));
    }

    // -------------------------------------------------------------
    // 3. EXPIRED JWT TEST
    // -------------------------------------------------------------
    @Test
    @DisplayName("3. Expired JWT on protected endpoint returns 401 Unauthorized")
    void testExpiredJwt() throws Exception {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date past = new Date(System.currentTimeMillis() - 100_000);
        String expiredToken = Jwts.builder()
                .setSubject("john_customer")
                .claim("userId", customerUser.getUserId())
                .claim("role", "CUSTOMER")
                .setIssuedAt(new Date(System.currentTimeMillis() - 200_000))
                .setExpiration(past)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        mockMvc.perform(get("/api/users/me")
                        .cookie(new Cookie("authToken", expiredToken)))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------
    // 4. INVALID / TAMPERED JWT TEST
    // -------------------------------------------------------------
    @Test
    @DisplayName("4. Tampered or invalid JWT signature returns 401 Unauthorized")
    void testInvalidJwt() throws Exception {
        String tamperedToken = customerToken + "tampered_signature";

        mockMvc.perform(get("/api/users/me")
                        .cookie(new Cookie("authToken", tamperedToken)))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------
    // 5. PROTECTED ENDPOINT TEST
    // -------------------------------------------------------------
    @Test
    @DisplayName("5. Accessing protected endpoint without token returns 401 Unauthorized")
    void testProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------
    // 6. PUBLIC ENDPOINT TEST
    // -------------------------------------------------------------
    @Test
    @DisplayName("6. Public catalog endpoints succeed without token, and with token without querying user DB")
    void testPublicEndpoints() throws Exception {
        // Public products without token
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(1)))
                .andExpect(jsonPath("$.products[0].name", is("Wireless Noise-Cancelling Headphones")))
                .andExpect(jsonPath("$.user.name", is("Guest")))
                .andExpect(jsonPath("$.user.role", is("GUEST")));

        // Public categories without token
        mockMvc.perform(get("/api/products/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].categoryName", is("Electronics")));

        // Public suggestions without token
        mockMvc.perform(get("/api/products/suggestions").param("q", "Wire"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions", hasSize(1)))
                .andExpect(jsonPath("$.suggestions[0].name", is("Wireless Noise-Cancelling Headphones")));

        // Dedicated filters endpoint
        mockMvc.perform(get("/api/products/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brands", hasItem("Sony")))
                .andExpect(jsonPath("$.categories", hasSize(1)));

        // Public products WITH valid token - extracts claims cryptographically without DB error
        mockMvc.perform(get("/api/products")
                        .cookie(new Cookie("authToken", customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name", is("john_customer")))
                .andExpect(jsonPath("$.user.role", is("CUSTOMER")));
    }

    // -------------------------------------------------------------
    // 7. ADMIN AUTHORIZATION TEST
    // -------------------------------------------------------------
    @Test
    @DisplayName("7. Admin user with ROLE_ADMIN accesses /admin/orders successfully")
    void testAdminAuthorizationSuccess() throws Exception {
        mockMvc.perform(get("/admin/orders")
                        .cookie(new Cookie("authToken", adminToken)))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------
    // 8. CUSTOMER AUTHORIZATION TEST (403 ON ADMIN ROUTE)
    // -------------------------------------------------------------
    @Test
    @DisplayName("8. Customer with ROLE_CUSTOMER accessing /admin/orders receives 403 Forbidden")
    void testCustomerForbiddenOnAdminEndpoint() throws Exception {
        mockMvc.perform(get("/admin/orders")
                        .cookie(new Cookie("authToken", customerToken)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------
    // 9. LOGOUT TEST
    // -------------------------------------------------------------
    @Test
    @DisplayName("9. Logout clears the authToken cookie, deletes server-side token record, and immediately invalidates the token")
    void testLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("authToken", customerToken)))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("authToken", 0))
                .andExpect(jsonPath("$.message", is("Logout successful")));

        // Verify token deleted from repository
        List<JWTToken> remainingTokens = jwtTokenRepository.findByUserId(customerUser.getUserId());
        assertThat(remainingTokens).isEmpty();

        // A copy of the same (still cryptographically valid, unexpired) token must be
        // rejected immediately after logout, not just once it naturally expires.
        mockMvc.perform(get("/api/users/me")
                        .cookie(new Cookie("authToken", customerToken)))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------
    // 13. LOGIN RATE LIMITING TEST
    // -------------------------------------------------------------
    @Test
    @DisplayName("13. Repeated login attempts beyond the limit are throttled with 429")
    void testLoginRateLimiting() throws Exception {
        String wrongPayload = """
            {
                "username": "john_customer",
                "password": "wrong_password"
            }
            """;

        for (int i = 0; i < 8; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(wrongPayload))
                    .andExpect(status().isUnauthorized());
        }

        // 9th attempt within the same window should be throttled
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongPayload))
                .andExpect(status().isTooManyRequests());
    }

    // -------------------------------------------------------------
    // 14. REGISTRATION VALIDATION AND MASS-ASSIGNMENT TEST
    // -------------------------------------------------------------
    @Test
    @DisplayName("14. Registration rejects weak passwords/invalid email and ignores a client-supplied role")
    void testRegistrationValidation() throws Exception {
        // Password too short
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username": "newbie", "email": "newbie@example.com", "password": "short"}
                            """))
                .andExpect(status().isBadRequest());

        // Invalid email format
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username": "newbie2", "email": "not-an-email", "password": "longenoughpw"}
                            """))
                .andExpect(status().isBadRequest());

        // A client-supplied "role" field must be ignored - new user is always CUSTOMER
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username": "sneaky", "email": "sneaky@example.com", "password": "longenoughpw", "role": "ADMIN"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username", is("sneaky")));

        User created = userRepository.findByUsername("sneaky").orElseThrow();
        assertThat(created.getRole()).isEqualTo(Role.CUSTOMER);
    }

    // -------------------------------------------------------------
    // 10. CART OPERATIONS & BATCH IMAGE FETCHING TEST
    // -------------------------------------------------------------
    @Test
    @DisplayName("10. Cart add, get with batch images, update quantity, and delete operations")
    void testCartWorkflow() throws Exception {
        // Add to cart
        String addPayload = String.format("{\"productId\": %d, \"quantity\": 2}", testProduct.getProductId());
        mockMvc.perform(post("/api/cart/add")
                        .cookie(new Cookie("authToken", customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addPayload))
                .andExpect(status().isCreated());

        // Get cart items - checks batch image fetch and correct overall total
        mockMvc.perform(get("/api/cart/items")
                        .cookie(new Cookie("authToken", customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("john_customer")))
                .andExpect(jsonPath("$.cart.products", hasSize(1)))
                .andExpect(jsonPath("$.cart.products[0].name", is("Wireless Noise-Cancelling Headphones")))
                .andExpect(jsonPath("$.cart.products[0].quantity", is(2)))
                .andExpect(jsonPath("$.cart.products[0].image_url", is("https://images.example.com/headphones.jpg")))
                .andExpect(jsonPath("$.cart.overall_total_price", is(29998.0)));

        // Update cart item quantity
        String updatePayload = String.format("{\"productId\": %d, \"quantity\": 4}", testProduct.getProductId());
        mockMvc.perform(put("/api/cart/update")
                        .cookie(new Cookie("authToken", customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk());

        // Verify count
        mockMvc.perform(get("/api/cart/items/count")
                        .cookie(new Cookie("authToken", customerToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("4"));

        // Delete cart item
        mockMvc.perform(delete("/api/cart/delete")
                        .param("productId", String.valueOf(testProduct.getProductId()))
                        .cookie(new Cookie("authToken", customerToken)))
                .andExpect(status().isNoContent());

        // Verify empty cart
        mockMvc.perform(get("/api/cart/items/count")
                        .cookie(new Cookie("authToken", customerToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    // -------------------------------------------------------------
    // 11. PRODUCT SEARCH AND FILTERING TEST
    // -------------------------------------------------------------
    @Test
    @DisplayName("11. Product prefix search, brand filter, and price range filter")
    void testProductSearchAndFiltering() throws Exception {
        // Search by keyword
        mockMvc.perform(get("/api/products").param("search", "Headphones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(1)))
                .andExpect(jsonPath("$.totalItems", is(1)));

        // Search with non-matching term
        mockMvc.perform(get("/api/products").param("search", "NonExistentKeyword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(0)))
                .andExpect(jsonPath("$.totalItems", is(0)));

        // Filter by brand
        mockMvc.perform(get("/api/products").param("brand", "Sony"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(1)));

        // Filter by price range
        mockMvc.perform(get("/api/products")
                        .param("minPrice", "10000")
                        .param("maxPrice", "20000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(1)));

        // Filter out of range
        mockMvc.perform(get("/api/products")
                        .param("minPrice", "20000")
                        .param("maxPrice", "50000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(0)));
    }

    // -------------------------------------------------------------
    // 12. ORDER ACCESS CONTROL TEST
    // -------------------------------------------------------------
    @Test
    @DisplayName("12. Customer cannot cancel another customer's order (access control)")
    void testOrderAccessControl() throws Exception {
        // Customer A creates an order
        Order order = new Order();
        order.setOrderId("order_cust_a_123");
        order.setUserId(customerUser.getUserId());
        order.setTotalAmount(BigDecimal.valueOf(14999.00));
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProductId(testProduct.getProductId());
        orderItem.setQuantity(1);
        orderItem.setPricePerUnit(testProduct.getPrice());
        orderItem.setTotalPrice(testProduct.getPrice());
        orderItemRepository.save(orderItem);

        // Other Customer (Customer B) attempts to cancel Customer A's order
        String cancelPayload = "{\"reason\": \"I want to cancel this order\"}";
        mockMvc.perform(post("/api/orders/order_cust_a_123/cancel")
                        .cookie(new Cookie("authToken", authService.generateToken(otherCustomer)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Unauthorized to cancel this order")));

        // Owner (Customer A) cancels successfully
        mockMvc.perform(post("/api/orders/order_cust_a_123/cancel")
                        .cookie(new Cookie("authToken", customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }
}
