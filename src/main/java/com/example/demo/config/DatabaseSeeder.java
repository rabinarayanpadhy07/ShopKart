package com.example.demo.config;

import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductImage;
import com.example.demo.entity.User;
import com.example.demo.entity.Role;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.seed.demo-data:false}")
    private boolean seedDemoData;

    @Value("${app.seed.admin:false}")
    private boolean seedAdmin;

    @Value("${app.seed.admin.username:}")
    private String seedAdminUsername;

    @Value("${app.seed.admin.email:}")
    private String seedAdminEmail;

    @Value("${app.seed.admin.password:}")
    private String seedAdminPassword;

    public DatabaseSeeder(CategoryRepository categoryRepository,
                          ProductRepository productRepository,
                          ProductImageRepository productImageRepository,
                          UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!seedDemoData && !seedAdmin) {
            return;
        }

        if (seedAdmin) {
            createSeedAdmin();
        }

        if (!seedDemoData) {
            return;
        }

        seedCategoryWithProducts("Shirts", List.of(
            new SeedProduct("Classic White Oxford Shirt", "Polo Ralph",
                "Timeless button-down oxford shirt in premium breathable cotton. Perfect for both office and casual weekends.",
                1899.00, 50, "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=300&q=75"),
            new SeedProduct("Casual Linen Summer Shirt", "Tommy Hilfiger",
                "Lightweight and breezy organic linen shirt. Features a relaxed collar and buttoned cuffs.",
                2499.00, 35, "https://images.unsplash.com/photo-1598033129183-c4f50c736f10?w=300&q=75"),
            new SeedProduct("Slim Fit Chambray Denim Shirt", "Levi's",
                "Authentic indigo-dyed chambray shirt with double chest pockets and durable metal snaps.",
                1699.00, 40, "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=300&q=75")
        ));

        seedCategoryWithProducts("Pants", List.of(
            new SeedProduct("Slim Fit Stretch Chino Pants", "Dockers",
                "Comfortable stretch cotton chinos. Wrinkle-resistant finish with a clean flat-front design.",
                2199.00, 45, "https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=300&q=75"),
            new SeedProduct("Classic 501 Original Fit Jeans", "Levi's",
                "The original straight leg jeans. Heavyweight non-stretch denim with the iconic button fly.",
                3499.00, 60, "https://images.unsplash.com/photo-1542272604-787c3835535d?w=300&q=75"),
            new SeedProduct("Relaxed Lightweight Cargo Pants", "Columbia",
                "Multi-pocket tactical cargo pants. Quick-dry nylon fabric with UPF 50 sun protection.",
                2799.00, 30, "https://images.unsplash.com/photo-1517423738875-5ce310acd3da?w=300&q=75")
        ));

        seedCategoryWithProducts("Accessories", List.of(
            new SeedProduct("Minimalist Leather Cardholder Wallet", "Bellroy",
                "Ultra-slim top grain leather wallet. Holds up to 8 cards with dedicated RFID protection.",
                1299.00, 80, "https://images.unsplash.com/photo-1627124712838-1a2a7dec33c4?w=300&q=75"),
            new SeedProduct("Classic Aviator Sunglasses", "Ray-Ban",
                "G-15 polarized green lenses with gold metal frame. Outstanding glare reduction and UV protection.",
                5999.00, 25, "https://images.unsplash.com/photo-1513909590959-157960e7eec6?w=300&q=75"),
            new SeedProduct("Water-Resistant Commuter Backpack", "Herschel",
                "15-inch laptop sleeve compartment, waterproof zippers, and signature striped fabric liner.",
                3999.00, 40, "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=300&q=75")
        ));

        seedCategoryWithProducts("Mobiles", List.of(
            new SeedProduct("iPhone 15 Pro Max", "Apple",
                "Titanium design, A17 Pro chip, 48MP main camera, and USB-C port. The peak of mobile technology.",
                139999.00, 15, "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=300&q=75"),
            new SeedProduct("Galaxy S24 Ultra", "Samsung",
                "Dynamic AMOLED 2X, built-in S Pen, Snapdragon 8 Gen 3, and advanced AI photo editing tools.",
                124999.00, 20, "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=300&q=75"),
            new SeedProduct("Pixel 8 Pro", "Google",
                "Super Actua display, Tensor G3 processor, and best-in-class low light Magic Eraser photography.",
                99999.00, 25, "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=300&q=75")
        ));

        seedCategoryWithProducts("Mobile Accessories", List.of(
            new SeedProduct("Magnetic Wireless Power Bank", "Anker",
                "10,000mAh mag-safe compatible portable charger with foldable stand for hands-free viewing.",
                2999.00, 90, "https://images.unsplash.com/photo-1609081219090-a6d81d3085bf?w=300&q=75"),
            new SeedProduct("Hybrid Shockproof Phone Case", "Spigen",
                "Military grade dual-layer protection with air cushion technology. Resists drops and scratches.",
                899.00, 150, "https://images.unsplash.com/photo-1541807084-5c52b6b3adef?w=300&q=75"),
            new SeedProduct("Dual USB-C 40W Fast Charger", "Anker",
                "Compact wall adapter powered by GaN technology. Intelligently allocates power to dual devices.",
                1499.00, 120, "https://images.unsplash.com/photo-1622445262465-2481c4574875?w=300&q=75")
        ));

        seedCategoryWithProducts("Beauty", List.of(
            new SeedProduct("Hydrating Hyaluronic Acid Serum", "The Ordinary",
                "Ultra-pure serum combining low, medium, and high molecular weight hyaluronic acid for deep hydration.",
                699.00, 200, "https://images.unsplash.com/photo-1608248597279-f99d160bfcbc?w=300&q=75"),
            new SeedProduct("Matte Liquid Lipstick Set", "M.A.C",
                "Super long-wearing pigment lipstick with a velvety matte finish that lasts up to 12 hours.",
                2190.00, 85, "https://images.unsplash.com/photo-1586495777744-4413f21062fa?w=300&q=75"),
            new SeedProduct("Mineral Sunscreen SPF 50", "La Roche-Posay",
                "Broad spectrum dry-touch face sunscreen. Fragrance-free and non-comedogenic for sensitive skin.",
                1850.00, 70, "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=300&q=75")
        ));

        seedCategoryWithProducts("Appliances", List.of(
            new SeedProduct("Digital Air Fryer 4L", "Philips",
                "Rapid Air technology with touchscreen control. Cook with up to 90% less oil for healthy meals.",
                7999.00, 40, "https://images.unsplash.com/photo-1621972750749-0fbb1abb7736?w=300&q=75"),
            new SeedProduct("Robotic Vacuum Cleaner", "Xiaomi",
                "LDS laser navigation with 4000Pa strong suction. Auto-docking, virtual walls, and app control.",
                19999.00, 18, "https://images.unsplash.com/photo-1518310383802-640c2de311b2?w=300&q=75"),
            new SeedProduct("Programmable Espresso Machine", "DeLonghi",
                "15-bar professional pressure pump with adjustable manual steam wand for rich, creamy lattes.",
                14999.00, 15, "https://images.unsplash.com/photo-1579888944880-d98341148721?w=300&q=75")
        ));

        seedCategoryWithProducts("Books", List.of(
            new SeedProduct("Atomic Habits", "James Clear",
                "An easy and proven way to build good habits and break bad ones. The million-copy bestseller.",
                499.00, 300, "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=300&q=75"),
            new SeedProduct("The Psychology of Money", "Morgan Housel",
                "Timeless lessons on wealth, greed, and happiness. Explore how people make financial choices.",
                399.00, 250, "https://images.unsplash.com/photo-1592496431122-2349e0fbc666?w=300&q=75"),
            new SeedProduct("Sapiens: A Brief History of Humankind", "Yuval Noah Harari",
                "Explore the historic forces that shaped humans from ancient tribes to modern technological societies.",
                599.00, 180, "https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=300&q=75")
        ));

        seedCategoryWithProducts("Food", List.of(
            new SeedProduct("Organic Roasted Almonds", "Nutty Gritties",
                "Premium roasted California almonds lightly salted. Gluten-free, rich in fiber and antioxidants.",
                449.00, 150, "https://images.unsplash.com/photo-1508061253366-f7da158b6d4f?w=300&q=75"),
            new SeedProduct("Pure Matcha Green Tea Powder", "Organica",
                "100% organic Japanese stone-ground matcha powder. Perfect for daily energy and weight loss tea.",
                999.00, 90, "https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=300&q=75"),
            new SeedProduct("Dark Chocolate Selection Box", "Lindt",
                "Assorted single-origin dark chocolate truffles with smooth melting cocoa centers.",
                750.00, 110, "https://images.unsplash.com/photo-1548907040-4d42b5212c10?w=300&q=75")
        ));
    }

    private void createSeedAdmin() {
        if (seedAdminUsername == null || seedAdminUsername.trim().isEmpty()
                || seedAdminEmail == null || seedAdminEmail.trim().isEmpty()
                || seedAdminPassword == null || seedAdminPassword.trim().isEmpty()) {
            throw new IllegalStateException("Seed admin requires SEED_ADMIN_USERNAME, SEED_ADMIN_EMAIL, and SEED_ADMIN_PASSWORD.");
        }
        
        Optional<User> existingAdminOpt = userRepository.findByEmail(seedAdminEmail.trim());
        if (existingAdminOpt.isPresent()) {
            User admin = existingAdminOpt.get();
            admin.setUsername(seedAdminUsername.trim());
            admin.setPassword(passwordEncoder.encode(seedAdminPassword));
            admin.setUpdatedAt(LocalDateTime.now());
            userRepository.save(admin);
            return;
        }

        User admin = new User();
        admin.setUsername(seedAdminUsername.trim());
        admin.setEmail(seedAdminEmail.trim());
        admin.setPassword(passwordEncoder.encode(seedAdminPassword));
        admin.setRole(Role.ADMIN);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        userRepository.save(admin);
    }

    private void seedCategoryWithProducts(String categoryName, List<SeedProduct> seedProducts) {
        Optional<Category> existingCategoryOpt = categoryRepository.findByCategoryName(categoryName);
        Category category;
        if (existingCategoryOpt.isEmpty()) {
            category = new Category();
            category.setCategoryName(categoryName);
            category = categoryRepository.save(category);
        } else {
            category = existingCategoryOpt.get();
        }

        // Only seed products if the category contains 0 products
        List<Product> products = productRepository.findByCategory_CategoryId(category.getCategoryId());
        if (products.isEmpty()) {
            for (SeedProduct sp : seedProducts) {
                Product product = new Product();
                product.setName(sp.name);
                product.setBrand(sp.brand);
                product.setDescription(sp.description);
                product.setPrice(BigDecimal.valueOf(sp.price));
                product.setStock(sp.stock);
                product.setCategory(category);
                product.setCreatedAt(LocalDateTime.now());
                product.setUpdatedAt(LocalDateTime.now());

                Product savedProduct = productRepository.save(product);

                ProductImage image = new ProductImage();
                image.setProduct(savedProduct);
                image.setImageUrl(sp.imageUrl);
                productImageRepository.save(image);
            }
        }
    }

    private static class SeedProduct {
        String name;
        String brand;
        String description;
        double price;
        int stock;
        String imageUrl;

        SeedProduct(String name, String brand, String description, double price, int stock, String imageUrl) {
            this.name = name;
            this.brand = brand;
            this.description = description;
            this.price = price;
            this.stock = stock;
            this.imageUrl = imageUrl;
        }
    }
}

