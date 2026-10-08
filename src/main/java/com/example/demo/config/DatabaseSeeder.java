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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    /**
     * Image URLs used by the original placeholder catalog. A product row that still
     * carries one of these is an untouched demo row and is upgraded in place to its
     * real replacement (keeping its ID so existing orders/carts stay valid). Rows
     * without them are owned by the admin and are never overwritten.
     */
    private static final Set<String> LEGACY_DEMO_IMAGE_URLS = Set.of(
            "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=300&q=75",
            "https://images.unsplash.com/photo-1598033129183-c4f50c736f10?w=300&q=75",
            "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=300&q=75",
            "https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=300&q=75",
            "https://images.unsplash.com/photo-1542272604-787c3835535d?w=300&q=75",
            "https://images.unsplash.com/photo-1517423738875-5ce310acd3da?w=300&q=75",
            "https://images.unsplash.com/photo-1627124712838-1a2a7dec33c4?w=300&q=75",
            "https://images.unsplash.com/photo-1513909590959-157960e7eec6?w=300&q=75",
            "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=300&q=75",
            "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=300&q=75",
            "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=300&q=75",
            "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=300&q=75",
            "https://images.unsplash.com/photo-1609081219090-a6d81d3085bf?w=300&q=75",
            "https://images.unsplash.com/photo-1541807084-5c52b6b3adef?w=300&q=75",
            "https://images.unsplash.com/photo-1622445262465-2481c4574875?w=300&q=75",
            "https://images.unsplash.com/photo-1608248597279-f99d160bfcbc?w=300&q=75",
            "https://images.unsplash.com/photo-1586495777744-4413f21062fa?w=300&q=75",
            "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=300&q=75",
            "https://images.unsplash.com/photo-1621972750749-0fbb1abb7736?w=300&q=75",
            "https://images.unsplash.com/photo-1518310383802-640c2de311b2?w=300&q=75",
            "https://images.unsplash.com/photo-1579888944880-d98341148721?w=300&q=75",
            "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=300&q=75",
            "https://images.unsplash.com/photo-1592496431122-2349e0fbc666?w=300&q=75",
            "https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=300&q=75",
            "https://images.unsplash.com/photo-1508061253366-f7da158b6d4f?w=300&q=75",
            "https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=300&q=75",
            "https://images.unsplash.com/photo-1548907040-4d42b5212c10?w=300&q=75",
            "https://images.pexels.com/photos/1043474/pexels-photo-1043474.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/1082529/pexels-photo-1082529.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/1092644/pexels-photo-1092644.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/1295572/pexels-photo-1295572.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/1598505/pexels-photo-1598505.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/1598507/pexels-photo-1598507.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/2905238/pexels-photo-2905238.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/297933/pexels-photo-297933.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/324028/pexels-photo-324028.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/3685530/pexels-photo-3685530.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/404280/pexels-photo-404280.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/40739/mobile-phone-case-handy-case-mobile-phone-40739.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/4107284/pexels-photo-4107284.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/4112553/pexels-photo-4112553.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/4219861/pexels-photo-4219861.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/4526407/pexels-photo-4526407.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/461428/pexels-photo-461428.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/46710/pexels-photo-46710.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/5698851/pexels-photo-5698851.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/788946/pexels-photo-788946.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/915915/pexels-photo-915915.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/918327/pexels-photo-918327.jpeg?auto=compress&cs=tinysrgb&w=800"
    );

    private static final List<SeedProduct> CATALOG = List.of(
            product("Mobiles", "Apple iPhone 13 Pro (128 GB) - Sierra Blue", "Apple", "6.1-inch Super Retina XDR display with ProMotion, A15 Bionic chip and a 12MP Pro camera system with macro photography and Cinematic mode.",
                69999, 15, List.of("https://cdn.dummyjson.com/product-images/smartphones/iphone-13-pro/1.webp", "https://cdn.dummyjson.com/product-images/smartphones/iphone-13-pro/2.webp", "https://cdn.dummyjson.com/product-images/smartphones/iphone-13-pro/3.webp"),
                List.of("iPhone 15 Pro Max")),
            product("Mobiles", "Samsung Galaxy S10 (8 GB RAM, 128 GB) - Prism Black", "Samsung", "6.1-inch Dynamic AMOLED Infinity-O display, ultrasonic in-display fingerprint sensor, triple rear camera and Wireless PowerShare.",
                34999, 12, List.of("https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s10/1.webp", "https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s10/2.webp", "https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s10/3.webp"),
                List.of("Galaxy S24 Ultra")),
            product("Mobiles", "OPPO F19 Pro+ 5G (8 GB RAM, 128 GB) - Space Silver", "OPPO", "6.43-inch AMOLED display, MediaTek Dimensity 800U 5G, 48MP AI quad camera and 50W Flash Charge.",
                25990, 20, List.of("https://cdn.dummyjson.com/product-images/smartphones/oppo-f19-pro-plus/1.webp", "https://cdn.dummyjson.com/product-images/smartphones/oppo-f19-pro-plus/2.webp", "https://cdn.dummyjson.com/product-images/smartphones/oppo-f19-pro-plus/3.webp"),
                List.of("Pixel 8 Pro")),
            product("Mobiles", "realme X (4 GB RAM, 128 GB)", "realme", "6.53-inch full-screen AMOLED display with pop-up selfie camera, 48MP Sony IMX586 rear camera and VOOC 3.0 fast charging.",
                14999, 25, List.of("https://cdn.dummyjson.com/product-images/smartphones/realme-x/1.webp", "https://cdn.dummyjson.com/product-images/smartphones/realme-x/2.webp", "https://cdn.dummyjson.com/product-images/smartphones/realme-x/3.webp"),
                List.of()),
            product("Mobiles", "realme XT (8 GB RAM, 128 GB) - Pearl Blue", "realme", "64MP quad camera, 6.4-inch Super AMOLED display, Snapdragon 712 and a 4000mAh battery with 20W VOOC charging.",
                15999, 25, List.of("https://cdn.dummyjson.com/product-images/smartphones/realme-xt/1.webp", "https://cdn.dummyjson.com/product-images/smartphones/realme-xt/2.webp", "https://cdn.dummyjson.com/product-images/smartphones/realme-xt/3.webp"),
                List.of()),
            product("Mobiles", "vivo X21 (6 GB RAM, 128 GB) - Ruby Red", "vivo", "In-display fingerprint scanning, 6.28-inch Super AMOLED display and a dual 12MP + 5MP AI camera.",
                17990, 18, List.of("https://cdn.dummyjson.com/product-images/smartphones/vivo-x21/1.webp", "https://cdn.dummyjson.com/product-images/smartphones/vivo-x21/2.webp", "https://cdn.dummyjson.com/product-images/smartphones/vivo-x21/3.webp"),
                List.of()),
            product("Tablets", "Apple iPad mini (6th Gen, Wi-Fi, 64 GB) - Starlight", "Apple", "8.3-inch Liquid Retina display, A15 Bionic chip, USB-C and support for Apple Pencil (2nd generation).",
                46900, 14, List.of("https://cdn.dummyjson.com/product-images/tablets/ipad-mini-2021-starlight/1.webp", "https://cdn.dummyjson.com/product-images/tablets/ipad-mini-2021-starlight/2.webp", "https://cdn.dummyjson.com/product-images/tablets/ipad-mini-2021-starlight/3.webp"),
                List.of()),
            product("Tablets", "Samsung Galaxy Tab S8+ (8 GB RAM, 128 GB, Wi-Fi) - Graphite", "Samsung", "12.4-inch Super AMOLED 120Hz display, Snapdragon 8 Gen 1 and an S Pen included in the box.",
                74999, 10, List.of("https://cdn.dummyjson.com/product-images/tablets/samsung-galaxy-tab-s8-plus-grey/1.webp", "https://cdn.dummyjson.com/product-images/tablets/samsung-galaxy-tab-s8-plus-grey/2.webp", "https://cdn.dummyjson.com/product-images/tablets/samsung-galaxy-tab-s8-plus-grey/3.webp"),
                List.of()),
            product("Laptops", "Apple MacBook Pro 14-inch (M1 Pro, 16 GB, 512 GB SSD) - Space Grey", "Apple", "14.2-inch Liquid Retina XDR display, 8-core M1 Pro chip, up to 17 hours of battery life, MagSafe 3, HDMI and an SDXC card slot.",
                169900, 8, List.of("https://cdn.dummyjson.com/product-images/laptops/apple-macbook-pro-14-inch-space-grey/1.webp", "https://cdn.dummyjson.com/product-images/laptops/apple-macbook-pro-14-inch-space-grey/2.webp", "https://cdn.dummyjson.com/product-images/laptops/apple-macbook-pro-14-inch-space-grey/3.webp"),
                List.of()),
            product("Laptops", "ASUS Zenbook Pro Duo 15 OLED (Core i9, 32 GB, 1 TB SSD)", "ASUS", "15.6-inch 4K OLED touch display plus a full-width ScreenPad Plus second screen, with NVIDIA GeForce RTX graphics.",
                249990, 5, List.of("https://cdn.dummyjson.com/product-images/laptops/asus-zenbook-pro-dual-screen-laptop/1.webp", "https://cdn.dummyjson.com/product-images/laptops/asus-zenbook-pro-dual-screen-laptop/2.webp", "https://cdn.dummyjson.com/product-images/laptops/asus-zenbook-pro-dual-screen-laptop/3.webp"),
                List.of()),
            product("Laptops", "HUAWEI MateBook X Pro (Core i7, 16 GB, 1 TB SSD)", "HUAWEI", "13.9-inch 3K touchscreen with a 91% screen-to-body ratio, all-metal unibody and a weight of under 1.4 kg.",
                124990, 6, List.of("https://cdn.dummyjson.com/product-images/laptops/huawei-matebook-x-pro/1.webp", "https://cdn.dummyjson.com/product-images/laptops/huawei-matebook-x-pro/2.webp", "https://cdn.dummyjson.com/product-images/laptops/huawei-matebook-x-pro/3.webp"),
                List.of()),
            product("Laptops", "Lenovo Yoga 920 2-in-1 (Core i7, 16 GB, 512 GB SSD)", "Lenovo", "13.9-inch 4K touchscreen convertible with a 360-degree watchband hinge, Lenovo Active Pen support and Thunderbolt 3.",
                99990, 7, List.of("https://cdn.dummyjson.com/product-images/laptops/lenovo-yoga-920/1.webp", "https://cdn.dummyjson.com/product-images/laptops/lenovo-yoga-920/2.webp", "https://cdn.dummyjson.com/product-images/laptops/lenovo-yoga-920/3.webp"),
                List.of()),
            product("Laptops", "Dell XPS 13 9300 (Core i7, 16 GB, 512 GB SSD)", "Dell", "13.4-inch InfinityEdge FHD+ display, 10th Gen Intel Core i7 and a CNC-machined aluminium and carbon fibre build.",
                134990, 9, List.of("https://cdn.dummyjson.com/product-images/laptops/new-dell-xps-13-9300-laptop/1.webp", "https://cdn.dummyjson.com/product-images/laptops/new-dell-xps-13-9300-laptop/2.webp", "https://cdn.dummyjson.com/product-images/laptops/new-dell-xps-13-9300-laptop/3.webp"),
                List.of()),
            product("Mobile Accessories", "Apple AirPods (3rd Generation) with Lightning Charging Case", "Apple", "Spatial audio with dynamic head tracking, Adaptive EQ, sweat and water resistance and up to 30 hours of listening with the case.",
                17990, 40, List.of("https://cdn.dummyjson.com/product-images/mobile-accessories/apple-airpods/1.webp", "https://cdn.dummyjson.com/product-images/mobile-accessories/apple-airpods/2.webp", "https://cdn.dummyjson.com/product-images/mobile-accessories/apple-airpods/3.webp"),
                List.of()),
            product("Mobile Accessories", "Apple AirPods Max - Silver", "Apple", "Over-ear headphones with Active Noise Cancellation, Transparency mode, spatial audio and up to 20 hours of battery life.",
                59900, 12, List.of("https://cdn.dummyjson.com/product-images/mobile-accessories/apple-airpods-max-silver/1.webp"),
                List.of()),
            product("Mobile Accessories", "Apple MagSafe Battery Pack", "Apple", "Snaps magnetically onto iPhone 12 and later for extra battery on the go; charges at up to 15W when plugged in.",
                9900, 25, List.of("https://cdn.dummyjson.com/product-images/mobile-accessories/apple-magsafe-battery-pack/1.webp", "https://cdn.dummyjson.com/product-images/mobile-accessories/apple-magsafe-battery-pack/2.webp"),
                List.of("Magnetic Wireless Power Bank")),
            product("Mobile Accessories", "Apple iPhone 12 / 12 Pro Silicone Case with MagSafe - Plum", "Apple", "Silky, soft-touch silicone exterior with a microfibre lining and built-in magnets that align with MagSafe chargers.",
                4900, 50, List.of("https://cdn.dummyjson.com/product-images/mobile-accessories/iphone-12-silicone-case-with-magsafe-plum/1.webp", "https://cdn.dummyjson.com/product-images/mobile-accessories/iphone-12-silicone-case-with-magsafe-plum/2.webp", "https://cdn.dummyjson.com/product-images/mobile-accessories/iphone-12-silicone-case-with-magsafe-plum/3.webp"),
                List.of("Hybrid Shockproof Phone Case")),
            product("Mobile Accessories", "Apple 5W USB Power Adapter with Lightning Cable", "Apple", "Compact USB power adapter and 1 m USB to Lightning cable for charging iPhone and AirPods.",
                1900, 80, List.of("https://cdn.dummyjson.com/product-images/mobile-accessories/apple-iphone-charger/1.webp", "https://cdn.dummyjson.com/product-images/mobile-accessories/apple-iphone-charger/2.webp"),
                List.of("Dual USB-C 40W Fast Charger")),
            product("Mobile Accessories", "Beats Flex Wireless Earphones - Flex Yellow", "Beats", "Apple W1 chip for easy pairing, magnetic earbuds that auto-play and pause, and up to 12 hours of battery life.",
                4999, 60, List.of("https://cdn.dummyjson.com/product-images/mobile-accessories/beats-flex-wireless-earphones/1.webp"),
                List.of()),
            product("Watches", "Apple Watch Series 4 (GPS, 40 mm) - Gold Aluminium with Sport Loop", "Apple", "ECG app, fall detection, a larger edge-to-edge display and an electrical heart sensor in a gold aluminium case.",
                29900, 10, List.of("https://cdn.dummyjson.com/product-images/mobile-accessories/apple-watch-series-4-gold/1.webp", "https://cdn.dummyjson.com/product-images/mobile-accessories/apple-watch-series-4-gold/2.webp", "https://cdn.dummyjson.com/product-images/mobile-accessories/apple-watch-series-4-gold/3.webp"),
                List.of()),
            product("Watches", "Longines Master Collection Automatic Chronograph", "Longines", "Self-winding chronograph in a stainless steel case and bracelet with a scratch-resistant sapphire crystal.",
                285000, 3, List.of("https://cdn.dummyjson.com/product-images/mens-watches/longines-master-collection/1.webp", "https://cdn.dummyjson.com/product-images/mens-watches/longines-master-collection/2.webp", "https://cdn.dummyjson.com/product-images/mens-watches/longines-master-collection/3.webp"),
                List.of()),
            product("Watches", "Rolex Cellini Date - Black Dial, 18 ct Everose Gold", "Rolex", "39 mm 18 ct Everose gold case with a black dial and leather strap, powered by the self-winding calibre 3165.",
                1650000, 2, List.of("https://cdn.dummyjson.com/product-images/mens-watches/rolex-cellini-date-black-dial/1.webp", "https://cdn.dummyjson.com/product-images/mens-watches/rolex-cellini-date-black-dial/2.webp", "https://cdn.dummyjson.com/product-images/mens-watches/rolex-cellini-date-black-dial/3.webp"),
                List.of()),
            product("Watches", "Rolex Datejust 41 - Oystersteel and Yellow Gold", "Rolex", "Fluted bezel, Jubilee bracelet and the signature Cyclops lens over the date, powered by calibre 3235.",
                1250000, 2, List.of("https://cdn.dummyjson.com/product-images/mens-watches/rolex-datejust/1.webp", "https://cdn.dummyjson.com/product-images/mens-watches/rolex-datejust/2.webp", "https://cdn.dummyjson.com/product-images/mens-watches/rolex-datejust/3.webp"),
                List.of()),
            product("Watches", "Rolex Submariner Date - Oystersteel and Yellow Gold", "Rolex", "41 mm diver's watch with a unidirectional rotatable Cerachrom bezel, waterproof to 300 metres.",
                1450000, 2, List.of("https://cdn.dummyjson.com/product-images/mens-watches/rolex-submariner-watch/1.webp", "https://cdn.dummyjson.com/product-images/mens-watches/rolex-submariner-watch/2.webp", "https://cdn.dummyjson.com/product-images/mens-watches/rolex-submariner-watch/3.webp"),
                List.of()),
            product("Watches", "IWC Ingenieur Automatic - Stainless Steel", "IWC Schaffhausen", "40 mm stainless steel case with a black dial, integrated bracelet and a soft-iron inner case for magnetic resistance.",
                980000, 2, List.of("https://cdn.dummyjson.com/product-images/womens-watches/iwc-ingenieur-automatic-steel/1.webp", "https://cdn.dummyjson.com/product-images/womens-watches/iwc-ingenieur-automatic-steel/2.webp", "https://cdn.dummyjson.com/product-images/womens-watches/iwc-ingenieur-automatic-steel/3.webp"),
                List.of()),
            product("Footwear", "Nike Air Jordan 1 Retro High OG - Red/Black/White", "Nike", "The 1985 original reissued in premium leather with Air cushioning in the heel and a classic high-top silhouette.",
                16995, 18, List.of("https://cdn.dummyjson.com/product-images/mens-shoes/nike-air-jordan-1-red-and-black/1.webp", "https://cdn.dummyjson.com/product-images/mens-shoes/nike-air-jordan-1-red-and-black/2.webp", "https://cdn.dummyjson.com/product-images/mens-shoes/nike-air-jordan-1-red-and-black/3.webp"),
                List.of()),
            product("Footwear", "PUMA Future Rider Play On Sneakers", "PUMA", "Retro running-inspired sneakers with a lightweight EVA midsole, nylon and suede upper and a rubber outsole.",
                6999, 30, List.of("https://cdn.dummyjson.com/product-images/mens-shoes/puma-future-rider-trainers/1.webp", "https://cdn.dummyjson.com/product-images/mens-shoes/puma-future-rider-trainers/2.webp", "https://cdn.dummyjson.com/product-images/mens-shoes/puma-future-rider-trainers/3.webp"),
                List.of()),
            product("Footwear", "Off-White Out Of Office Sneakers - White/Red", "Off-White", "Low-top leather sneakers with the signature arrow motif, zip-tie tag and a chunky rubber sole.",
                52000, 4, List.of("https://cdn.dummyjson.com/product-images/mens-shoes/sports-sneakers-off-white-%26-red/1.webp", "https://cdn.dummyjson.com/product-images/mens-shoes/sports-sneakers-off-white-%26-red/2.webp", "https://cdn.dummyjson.com/product-images/mens-shoes/sports-sneakers-off-white-%26-red/3.webp"),
                List.of()),
            product("Footwear", "Calvin Klein Women's Heeled Sandals - Black", "Calvin Klein", "Sleek black heeled sandals with logo-embossed straps and a cushioned footbed.",
                8999, 15, List.of("https://cdn.dummyjson.com/product-images/womens-shoes/calvin-klein-heel-shoes/1.webp", "https://cdn.dummyjson.com/product-images/womens-shoes/calvin-klein-heel-shoes/2.webp", "https://cdn.dummyjson.com/product-images/womens-shoes/calvin-klein-heel-shoes/3.webp"),
                List.of()),
            product("Fragrances", "Calvin Klein CK One Eau de Toilette 100 ml", "Calvin Klein", "The iconic unisex citrus-aromatic scent with notes of bergamot, green tea and musk.",
                4500, 40, List.of("https://cdn.dummyjson.com/product-images/fragrances/calvin-klein-ck-one/1.webp", "https://cdn.dummyjson.com/product-images/fragrances/calvin-klein-ck-one/2.webp", "https://cdn.dummyjson.com/product-images/fragrances/calvin-klein-ck-one/3.webp"),
                List.of()),
            product("Fragrances", "Chanel Coco Noir Eau de Parfum 100 ml", "Chanel", "A deep oriental fragrance of bergamot, rose, patchouli and sandalwood in the signature black bottle.",
                15950, 8, List.of("https://cdn.dummyjson.com/product-images/fragrances/chanel-coco-noir-eau-de/1.webp", "https://cdn.dummyjson.com/product-images/fragrances/chanel-coco-noir-eau-de/2.webp", "https://cdn.dummyjson.com/product-images/fragrances/chanel-coco-noir-eau-de/3.webp"),
                List.of()),
            product("Fragrances", "Dior J'adore Eau de Parfum 100 ml", "Dior", "A luminous floral bouquet of ylang-ylang, Damascus rose and Grasse jasmine.",
                14400, 10, List.of("https://cdn.dummyjson.com/product-images/fragrances/dior-j%27adore/1.webp", "https://cdn.dummyjson.com/product-images/fragrances/dior-j%27adore/2.webp", "https://cdn.dummyjson.com/product-images/fragrances/dior-j%27adore/3.webp"),
                List.of()),
            product("Fragrances", "Dolce & Gabbana Dolce Shine Eau de Parfum 75 ml", "Dolce & Gabbana", "A radiant fruity floral with mango, white flowers and blond woods.",
                8900, 12, List.of("https://cdn.dummyjson.com/product-images/fragrances/dolce-shine-eau-de/1.webp", "https://cdn.dummyjson.com/product-images/fragrances/dolce-shine-eau-de/2.webp", "https://cdn.dummyjson.com/product-images/fragrances/dolce-shine-eau-de/3.webp"),
                List.of()),
            product("Fragrances", "Gucci Bloom Eau de Parfum 100 ml", "Gucci", "A rich white floral of tuberose, jasmine and Rangoon creeper.",
                11500, 10, List.of("https://cdn.dummyjson.com/product-images/fragrances/gucci-bloom-eau-de/1.webp", "https://cdn.dummyjson.com/product-images/fragrances/gucci-bloom-eau-de/2.webp", "https://cdn.dummyjson.com/product-images/fragrances/gucci-bloom-eau-de/3.webp"),
                List.of()),
            product("Beauty", "essence Lash Princess False Lash Effect Mascara", "essence", "Cult-favourite vegan mascara with a conic fibre brush for dramatic volume and length.",
                399, 150, List.of("https://cdn.dummyjson.com/product-images/beauty/essence-mascara-lash-princess/1.webp"),
                List.of("Hydrating Hyaluronic Acid Serum")),
            product("Beauty", "Olay Ultra Moisture Shea Butter Body Wash 650 ml", "Olay", "Creamy body wash with shea butter and vitamin B3 complex that leaves skin soft and hydrated.",
                899, 80, List.of("https://cdn.dummyjson.com/product-images/skin-care/olay-ultra-moisture-shea-butter-body-wash/1.webp", "https://cdn.dummyjson.com/product-images/skin-care/olay-ultra-moisture-shea-butter-body-wash/2.webp", "https://cdn.dummyjson.com/product-images/skin-care/olay-ultra-moisture-shea-butter-body-wash/3.webp"),
                List.of("Matte Liquid Lipstick Set")),
            product("Beauty", "Vaseline Men Fast Absorbing Body & Face Lotion 400 ml", "Vaseline", "Non-greasy lotion for men that absorbs quickly and hydrates both body and face.",
                449, 100, List.of("https://cdn.dummyjson.com/product-images/skin-care/vaseline-men-body-and-face-lotion/1.webp", "https://cdn.dummyjson.com/product-images/skin-care/vaseline-men-body-and-face-lotion/2.webp", "https://cdn.dummyjson.com/product-images/skin-care/vaseline-men-body-and-face-lotion/3.webp"),
                List.of("Mineral Sunscreen SPF 50")),
            product("Beauty", "ATTITUDE Super Leaves Natural Hand Soap - Lemon Leaves 473 ml", "ATTITUDE", "Plant-based, hypoallergenic hand soap with olive leaf extract and a fresh lemon scent.",
                799, 60, List.of("https://cdn.dummyjson.com/product-images/skin-care/attitude-super-leaves-hand-soap/1.webp", "https://cdn.dummyjson.com/product-images/skin-care/attitude-super-leaves-hand-soap/2.webp", "https://cdn.dummyjson.com/product-images/skin-care/attitude-super-leaves-hand-soap/3.webp"),
                List.of()),
            product("Accessories", "Prada Galleria Saffiano Leather Bag - Light Blue", "Prada", "Structured Saffiano leather handbag with the enamel triangle logo, double handles and a detachable shoulder strap.",
                285000, 3, List.of("https://cdn.dummyjson.com/product-images/womens-bags/prada-women-bag/1.webp", "https://cdn.dummyjson.com/product-images/womens-bags/prada-women-bag/2.webp", "https://cdn.dummyjson.com/product-images/womens-bags/prada-women-bag/3.webp"),
                List.of()),
            product("Accessories", "HESHE Women's Genuine Leather Shoulder Bag - Brown", "HESHE", "Soft genuine leather shoulder bag with a zip-top closure and multiple interior pockets.",
                6499, 20, List.of("https://cdn.dummyjson.com/product-images/womens-bags/heshe-women%27s-leather-bag/1.webp", "https://cdn.dummyjson.com/product-images/womens-bags/heshe-women%27s-leather-bag/2.webp", "https://cdn.dummyjson.com/product-images/womens-bags/heshe-women%27s-leather-bag/3.webp"),
                List.of("Minimalist Leather Cardholder Wallet")),
            product("Accessories", "Women's Structured Tote Handbag - Navy", null, "Spacious structured tote with top handles and a zip compartment for everyday essentials.",
                2499, 25, List.of("https://cdn.dummyjson.com/product-images/womens-bags/blue-women%27s-handbag/1.webp", "https://cdn.dummyjson.com/product-images/womens-bags/blue-women%27s-handbag/2.webp", "https://cdn.dummyjson.com/product-images/womens-bags/blue-women%27s-handbag/3.webp"),
                List.of()),
            product("Accessories", "Faux Leather Mini Backpack - White", null, "Compact drawstring backpack in vegan leather with a flap closure and adjustable straps.",
                1999, 30, List.of("https://cdn.dummyjson.com/product-images/womens-bags/white-faux-leather-backpack/1.webp", "https://cdn.dummyjson.com/product-images/womens-bags/white-faux-leather-backpack/2.webp", "https://cdn.dummyjson.com/product-images/womens-bags/white-faux-leather-backpack/3.webp"),
                List.of("Water-Resistant Commuter Backpack")),
            product("Accessories", "Classic Metal Aviator Sunglasses - Blue Gradient", null, "Lightweight metal aviator frame with UV400 gradient lenses.",
                1499, 40, List.of("https://cdn.dummyjson.com/product-images/sunglasses/classic-sun-glasses/1.webp", "https://cdn.dummyjson.com/product-images/sunglasses/classic-sun-glasses/2.webp", "https://cdn.dummyjson.com/product-images/sunglasses/classic-sun-glasses/3.webp"),
                List.of("Classic Aviator Sunglasses")),
            product("Accessories", "Browline Sunglasses - Tortoise", null, "Retro browline frame in tortoiseshell with green UV400 lenses.",
                1299, 40, List.of("https://cdn.dummyjson.com/product-images/sunglasses/black-sun-glasses/1.webp", "https://cdn.dummyjson.com/product-images/sunglasses/black-sun-glasses/2.webp", "https://cdn.dummyjson.com/product-images/sunglasses/black-sun-glasses/3.webp"),
                List.of()),
            product("Shirts", "Men's Slim Fit Gingham Check Shirt - Blue/White", null, "Breathable cotton shirt in a classic gingham check with a button-down collar and slim fit.",
                1299, 45, List.of("https://cdn.dummyjson.com/product-images/mens-shirts/blue-%26-black-check-shirt/1.webp", "https://cdn.dummyjson.com/product-images/mens-shirts/blue-%26-black-check-shirt/2.webp", "https://cdn.dummyjson.com/product-images/mens-shirts/blue-%26-black-check-shirt/3.webp"),
                List.of("Casual Linen Summer Shirt")),
            product("Shirts", "Men's Buffalo Check Flannel Shirt - Red/Black", null, "Brushed cotton flannel in a bold buffalo check, finished with two chest pockets.",
                1499, 40, List.of("https://cdn.dummyjson.com/product-images/mens-shirts/man-plaid-shirt/1.webp", "https://cdn.dummyjson.com/product-images/mens-shirts/man-plaid-shirt/2.webp", "https://cdn.dummyjson.com/product-images/mens-shirts/man-plaid-shirt/3.webp"),
                List.of("Slim Fit Chambray Denim Shirt")),
            product("Shirts", "Men's Hawaiian Floral Print Shirt - Blue", null, "Relaxed short-sleeve camp collar shirt in an all-over tropical floral print.",
                1199, 35, List.of("https://cdn.dummyjson.com/product-images/mens-shirts/man-short-sleeve-shirt/1.webp", "https://cdn.dummyjson.com/product-images/mens-shirts/man-short-sleeve-shirt/2.webp", "https://cdn.dummyjson.com/product-images/mens-shirts/man-short-sleeve-shirt/3.webp"),
                List.of("Classic White Oxford Shirt")),
            product("Shirts", "Men's Tartan Check Casual Shirt - Teal/Beige", null, "Soft cotton casual shirt in a muted tartan check with a regular fit.",
                1299, 35, List.of("https://cdn.dummyjson.com/product-images/mens-shirts/men-check-shirt/1.webp", "https://cdn.dummyjson.com/product-images/mens-shirts/men-check-shirt/2.webp", "https://cdn.dummyjson.com/product-images/mens-shirts/men-check-shirt/3.webp"),
                List.of()),
            product("Shirts", "GIGABYTE AORUS Graphic T-Shirt - White", "GIGABYTE", "Crew-neck cotton tee with the AORUS eagle graphic, made for gamers.",
                999, 50, List.of("https://cdn.dummyjson.com/product-images/mens-shirts/gigabyte-aorus-men-tshirt/1.webp", "https://cdn.dummyjson.com/product-images/mens-shirts/gigabyte-aorus-men-tshirt/2.webp", "https://cdn.dummyjson.com/product-images/mens-shirts/gigabyte-aorus-men-tshirt/3.webp"),
                List.of("Relaxed Lightweight Cargo Pants")),
            product("Pants", "Levi's 501 Original Fit Jeans", "Levi's", "The original straight-leg jean since 1873, in heavyweight non-stretch denim with the iconic button fly.",
                3999, 60, List.of("https://images.unsplash.com/photo-1542272604-787c3835535d?w=800&q=80"),
                List.of("Classic 501 Original Fit Jeans")),
            product("Pants", "Men's Straight Fit Dark Wash Jeans", null, "Everyday straight-fit jeans in a deep indigo wash with a classic five-pocket design.",
                1799, 45, List.of("https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=800&q=80"),
                List.of("Slim Fit Stretch Chino Pants")),
            product("Books", "Atomic Habits", "James Clear", "An easy and proven way to build good habits and break bad ones. The international bestseller.",
                499, 300, List.of("https://covers.openlibrary.org/b/isbn/9780735211292-L.jpg"),
                List.of()),
            product("Books", "The Psychology of Money", "Morgan Housel", "Timeless lessons on wealth, greed and happiness, and how people really make financial decisions.",
                399, 250, List.of("https://covers.openlibrary.org/b/isbn/9780857197689-L.jpg"),
                List.of()),
            product("Books", "Sapiens: A Brief History of Humankind", "Yuval Noah Harari", "How Homo sapiens came to dominate the planet, from the Cognitive Revolution to the present day.",
                599, 180, List.of("https://covers.openlibrary.org/b/isbn/9780062316097-L.jpg"),
                List.of()),
            product("Books", "Ikigai: The Japanese Secret to a Long and Happy Life", "Hector Garcia & Francesc Miralles", "Discover your ikigai, your reason for being, through the habits of the world's longest-living people.",
                399, 200, List.of("https://covers.openlibrary.org/b/isbn/9780143130727-L.jpg"),
                List.of()),
            product("Books", "Deep Work", "Cal Newport", "Rules for focused success in a distracted world.",
                499, 150, List.of("https://covers.openlibrary.org/b/isbn/9781455586691-L.jpg"),
                List.of()),
            product("Books", "The Alchemist", "Paulo Coelho", "The enchanting fable of Santiago's journey in search of treasure and his Personal Legend. 25th anniversary edition.",
                299, 220, List.of("https://covers.openlibrary.org/b/isbn/9780062315007-L.jpg"),
                List.of()),
            product("Books", "Rich Dad Poor Dad", "Robert T. Kiyosaki", "What the rich teach their kids about money that the poor and middle class do not.",
                399, 200, List.of("https://covers.openlibrary.org/b/isbn/9781612680194-L.jpg"),
                List.of()),
            product("Books", "Thinking, Fast and Slow", "Daniel Kahneman", "The Nobel laureate's tour of the two systems that drive the way we think.",
                599, 120, List.of("https://covers.openlibrary.org/b/isbn/9780374533557-L.jpg"),
                List.of()),
            product("Books", "Project Hail Mary", "Andy Weir", "A lone astronaut must save the earth from disaster, from the author of The Martian.",
                699, 90, List.of("https://covers.openlibrary.org/b/isbn/9780593135204-L.jpg"),
                List.of()),
            product("Books", "The Midnight Library", "Matt Haig", "Between life and death there is a library, and every book offers a chance to try another life.",
                499, 110, List.of("https://covers.openlibrary.org/b/isbn/9780525559474-L.jpg"),
                List.of()),
            product("Food", "NESCAFE Clasico Instant Coffee 200 g", "Nestle", "100% pure dark roast instant coffee with a bold, full-bodied flavour.",
                699, 120, List.of("https://cdn.dummyjson.com/product-images/groceries/nescafe-coffee/1.webp"),
                List.of("Pure Matcha Green Tea Powder")),
            product("Food", "Optimum Nutrition Performance Whey Protein - 2 lb", "Optimum Nutrition", "Whey protein blend with 22 g of protein per serving to support muscle recovery.",
                3499, 40, List.of("https://cdn.dummyjson.com/product-images/groceries/protein-powder/1.webp"),
                List.of("Dark Chocolate Selection Box")),
            product("Food", "Raw Wildflower Honey 500 g", null, "Unprocessed raw honey collected from wildflower meadows, with no added sugar.",
                449, 90, List.of("https://cdn.dummyjson.com/product-images/groceries/honey-jar/1.webp"),
                List.of("Organic Roasted Almonds")),
            product("Food", "Cold-Pressed Orange Juice 1 L", null, "100% orange juice with no added sugar or preservatives.",
                179, 70, List.of("https://cdn.dummyjson.com/product-images/groceries/juice/1.webp"),
                List.of()),
            product("Food", "Refined Sunflower Oil 1 L", null, "Light, heart-friendly sunflower oil for everyday cooking and frying.",
                189, 100, List.of("https://cdn.dummyjson.com/product-images/groceries/cooking-oil/1.webp"),
                List.of()),
            product("Food", "Premium Aged Basmati Rice 1 kg", null, "Long-grain aged basmati rice that cooks up fluffy and aromatic.",
                199, 150, List.of("https://cdn.dummyjson.com/product-images/groceries/rice/1.webp"),
                List.of()),
            product("Appliances", "Amazon Echo Plus (2nd Gen) Smart Speaker with Alexa - Charcoal", "Amazon", "Premium 360-degree sound with a built-in smart home hub and temperature sensor. Just ask Alexa.",
                14999, 20, List.of("https://cdn.dummyjson.com/product-images/mobile-accessories/amazon-echo-plus/1.webp", "https://cdn.dummyjson.com/product-images/mobile-accessories/amazon-echo-plus/2.webp"),
                List.of("Robotic Vacuum Cleaner")),
            product("Appliances", "Apple HomePod mini - Space Grey", "Apple", "Room-filling 360-degree audio with Siri, Intercom and smart home control in a compact design.",
                10900, 20, List.of("https://cdn.dummyjson.com/product-images/mobile-accessories/apple-homepod-mini-cosmic-grey/1.webp"),
                List.of()),
            product("Appliances", "Countertop Blender 1.5 L, 1000 W", null, "Powerful 1000 W motor with stainless steel blades and a 1.5 L jar for smoothies, shakes and purees.",
                3999, 25, List.of("https://cdn.dummyjson.com/product-images/kitchen-accessories/boxed-blender/1.webp", "https://cdn.dummyjson.com/product-images/kitchen-accessories/boxed-blender/2.webp", "https://cdn.dummyjson.com/product-images/kitchen-accessories/boxed-blender/3.webp"),
                List.of("Digital Air Fryer 4L")),
            product("Appliances", "Hand Blender with Stainless Steel Shaft, 600 W", null, "Ergonomic hand blender with a detachable stainless steel shaft for soups, sauces and smoothies.",
                1999, 40, List.of("https://cdn.dummyjson.com/product-images/kitchen-accessories/hand-blender/1.webp"),
                List.of()),
            product("Appliances", "Solo Microwave Oven 20 L - White", null, "20 L solo microwave with 5 power levels, a defrost function and a 30-minute timer.",
                6499, 15, List.of("https://cdn.dummyjson.com/product-images/kitchen-accessories/microwave-oven/1.webp", "https://cdn.dummyjson.com/product-images/kitchen-accessories/microwave-oven/2.webp", "https://cdn.dummyjson.com/product-images/kitchen-accessories/microwave-oven/3.webp"),
                List.of("Programmable Espresso Machine")),
            product("Appliances", "Built-in Induction Hob, 4 Cooking Zones", null, "Black glass-ceramic built-in hob with four induction zones and touch controls.",
                18999, 8, List.of("https://cdn.dummyjson.com/product-images/kitchen-accessories/electric-stove/1.webp", "https://cdn.dummyjson.com/product-images/kitchen-accessories/electric-stove/2.webp", "https://cdn.dummyjson.com/product-images/kitchen-accessories/electric-stove/3.webp"),
                List.of())
    );

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (seedAdmin) {
            createSeedAdmin();
        }
        if (seedDemoData) {
            seedCatalog();
        }

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

    /**
     * Idempotent catalog upsert: creates missing products, upgrades legacy demo rows
     * in place, and leaves everything else (admin-managed products) untouched.
     */
    private void seedCatalog() {
        Map<String, Product> byName = new HashMap<>();
        for (Product p : productRepository.findAll()) {
            byName.putIfAbsent(key(p.getName()), p);
        }

        Set<Integer> legacyIds = new HashSet<>();
        List<Integer> allIds = byName.values().stream().map(Product::getProductId).toList();
        if (!allIds.isEmpty()) {
            for (ProductImage image : productImageRepository.findByProduct_ProductIdIn(allIds)) {
                if (LEGACY_DEMO_IMAGE_URLS.contains(image.getImageUrl())) {
                    legacyIds.add(image.getProduct().getProductId());
                }
            }
        }

        Map<String, Category> categories = new HashMap<>();
        int created = 0;
        int upgraded = 0;
        for (SeedProduct sp : CATALOG) {
            Category category = categories.computeIfAbsent(sp.category(), this::findOrCreateCategory);

            Product existing = byName.get(key(sp.name()));
            if (existing == null) {
                for (String legacyName : sp.replaces()) {
                    existing = byName.get(key(legacyName));
                    if (existing != null) break;
                }
            }

            if (existing == null) {
                Product product = new Product();
                product.setCreatedAt(LocalDateTime.now());
                apply(product, sp, category);
                byName.put(key(sp.name()), product);
                created++;
            } else if (legacyIds.remove(existing.getProductId())) {
                apply(existing, sp, category);
                byName.put(key(sp.name()), existing);
                upgraded++;
            }
        }

        if (created > 0 || upgraded > 0) {
            logger.info("Catalog seed: {} products created, {} demo products upgraded", created, upgraded);
        }
    }

    private void apply(Product product, SeedProduct sp, Category category) {
        product.setName(sp.name());
        product.setBrand(sp.brand());
        product.setDescription(sp.description());
        product.setPrice(BigDecimal.valueOf(sp.price()));
        product.setStock(sp.stock());
        product.setCategory(category);
        product.setUpdatedAt(LocalDateTime.now());
        Product saved = productRepository.save(product);

        productImageRepository.deleteByProductId(saved.getProductId());
        for (String url : sp.images()) {
            ProductImage image = new ProductImage();
            image.setProduct(saved);
            image.setImageUrl(url);
            productImageRepository.save(image);
        }
    }

    private Category findOrCreateCategory(String name) {
        return categoryRepository.findByCategoryName(name).orElseGet(() -> {
            Category category = new Category();
            category.setCategoryName(name);
            return categoryRepository.save(category);
        });
    }

    private static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static SeedProduct product(String category, String name, String brand, String description,
                                       long price, int stock, List<String> images, List<String> replaces) {
        return new SeedProduct(category, name, brand, description, price, stock, images, replaces);
    }

    private record SeedProduct(String category, String name, String brand, String description,
                               long price, int stock, List<String> images, List<String> replaces) {
    }
}
