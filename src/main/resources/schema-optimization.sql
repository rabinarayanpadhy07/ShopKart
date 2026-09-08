-- =======================================================================
-- ShopKart Production Performance Indexing & Schema Optimization Migration
-- Compatible with MySQL 8.0+
-- Run this script explicitly against the production database 'salessavvy'
-- =======================================================================

-- 1. Users Table Indexes
-- Accelerates username lookups during authentication and email lookups for Google auth
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 2. JWT Tokens Table Indexes
-- Eliminates full table scans during token lookup, session pruning, and expiration checks
ALTER TABLE jwt_tokens MODIFY COLUMN token VARCHAR(1000) NOT NULL;
CREATE INDEX IF NOT EXISTS idx_jwt_tokens_token ON jwt_tokens(token(255));
CREATE INDEX IF NOT EXISTS idx_jwt_tokens_user_id ON jwt_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_jwt_tokens_expires_at ON jwt_tokens(expires_at);

-- 3. Cart Items Table Indexes
-- Accelerates cart item retrieval by user and point lookups for cart mutations
CREATE INDEX IF NOT EXISTS idx_cart_items_user_id ON cart_items(user_id);
CREATE INDEX IF NOT EXISTS idx_cart_user_product ON cart_items(user_id, product_id);

-- 4. Products Table Indexes
-- Eliminates table scans during storefront catalog browsing, filtering, and prefix searches
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_brand ON products(brand);
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);
CREATE INDEX IF NOT EXISTS idx_products_rating ON products(average_rating);
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_created_at ON products(created_at);

-- 5. Product Images Table Indexes
-- Accelerates batch image retrieval for product cards in catalog, cart, and orders
CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON productimages(product_id);

-- 6. Orders Table Indexes
-- Speeds up customer order history queries and admin sales analytics aggregations
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);

-- 7. Order Items Table Indexes
-- Speeds up order line item fetching, purchase verification for reviews, and stock adjustments
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);

-- 8. Order Status History Table Indexes
-- Accelerates order audit history timeline lookups
CREATE INDEX IF NOT EXISTS idx_order_status_history_order_id ON order_status_history(order_id);

-- 9. Reviews Table Indexes
-- Accelerates product review listings and average rating calculations
CREATE INDEX IF NOT EXISTS idx_reviews_product_id ON reviews(product_id);
CREATE INDEX IF NOT EXISTS idx_reviews_user_product ON reviews(user_id, product_id);

-- 10. Addresses Table Indexes
-- Speeds up checkout address lookups for authenticated users
CREATE INDEX IF NOT EXISTS idx_addresses_user_id ON addresses(user_id);
