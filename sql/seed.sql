-- =======================================================
-- ZUDIO – Online Shopping Management System
-- Seed Data: Categories, Products, Users, Coupons, Orders
-- =======================================================

USE zudio_db;

-- 1. Seed Categories
INSERT INTO categories (id, name, description, image_url) VALUES
(1, 'Men', 'Trendy casuals, streetwear, tees, chinos & jackets for modern men', 'https://images.unsplash.com/photo-1516257984-b1b4d707412e?auto=format&fit=crop&w=800&q=80'),
(2, 'Women', 'Chic ethnic wear, oversized hoodies, dresses & crop tops', 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=800&q=80'),
(3, 'Kids', 'Playful, comfortable & vibrant everyday clothing for boys & girls', 'https://images.unsplash.com/photo-1622290291468-a28f7a7dc6a8?auto=format&fit=crop&w=800&q=80'),
(4, 'Footwear', 'Chunky retro sneakers, sliders, slip-ons & casual footwear', 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=800&q=80'),
(5, 'Accessories', 'Caps, backpacks, crossbody bags, socks & statement eyewear', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&q=80')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 2. Seed Users
-- User accounts (Admin and Sample Customers) are dynamically created on first startup
-- by DatabaseUtil using PBKDF2WithHmacSHA256 with random per-user salts and environment credentials.

-- 3. Seed Coupons
INSERT INTO coupons (code, discount_percent, active, min_order_amount) VALUES
('ZUDIO10', 10, TRUE, 499.00),
('ZUDIO20', 20, TRUE, 999.00),
('ZUDIO50', 50, TRUE, 1499.00),
('FREESHIP', 5, TRUE, 299.00)
ON DUPLICATE KEY UPDATE discount_percent=VALUES(discount_percent);

-- 4. Seed Products (32 Realistic Items with Zudio pricing: ₹199 to ₹1499)
INSERT INTO products (id, name, description, price, mrp, category_id, image_url, stock, rating, is_trending) VALUES
-- Men Category
(1, 'Oversized Boxy Acid Wash Graphic Tee', 'Heavyweight 240 GSM pure cotton oversized graphic t-shirt with ribbed crew neck and dropped shoulders.', 499.00, 799.00, 1, 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=800&q=80', 45, 4.7, TRUE),
(2, 'Relaxed Fit Utility Cargo Joggers', 'Tactical cargo trousers with 6 deep utility pockets, elasticated drawstring waistband, and cuffed ankles.', 999.00, 1499.00, 1, 'https://images.unsplash.com/photo-1517445312882-bc9910d016b7?auto=format&fit=crop&w=800&q=80', 28, 4.5, TRUE),
(3, 'Cuban Collar Textured Resort Shirt', 'Breathable waffle weave cotton casual shirt with open resort collar and wooden effect buttons.', 699.00, 999.00, 1, 'https://images.unsplash.com/photo-1596755094514-f87e34085b2c?auto=format&fit=crop&w=800&q=80', 35, 4.6, FALSE),
(4, 'Vintage Washed Denim Trucker Jacket', 'Classic rugged blue denim jacket with contrast stitch detailing, twin flap chest pockets and metal buttons.', 1299.00, 1999.00, 1, 'https://images.unsplash.com/photo-1576871337632-b9aef4c17ab9?auto=format&fit=crop&w=800&q=80', 18, 4.8, TRUE),
(5, 'French Terry Minimalist Crewneck Sweatshirt', 'Super-soft brushed fleece pullover sweatshirt in aesthetic beige shade for effortless layering.', 799.00, 1199.00, 1, 'https://images.unsplash.com/photo-1556905055-8f358a7a47b2?auto=format&fit=crop&w=800&q=80', 50, 4.4, FALSE),
(6, 'Slim Tapered Stretch Chino Trousers', 'Premium cotton-lycra blend breathable chinos tailored for semi-casual wear and all-day comfort.', 899.00, 1299.00, 1, 'https://images.unsplash.com/photo-1473966968600-fa801b869a1a?auto=format&fit=crop&w=800&q=80', 22, 4.3, FALSE),
(7, 'Striped Knitted Polo T-Shirt', 'Retro inspired knit polo featuring contrast collar and placket, ribbed sleeves and breathable cotton finish.', 599.00, 899.00, 1, 'https://images.unsplash.com/photo-1625910513413-7e44795b5463?auto=format&fit=crop&w=800&q=80', 40, 4.5, TRUE),

-- Women Category
(8, 'Floral Print A-Line Wrap Midi Dress', 'Breezy chiffon floral wrap dress with flutter sleeves, tie-up waist belt and soft inner lining.', 799.00, 1299.00, 2, 'https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?auto=format&fit=crop&w=800&q=80', 32, 4.8, TRUE),
(9, 'Cropped Ribbed Henley Long Sleeve Top', 'Stretchy ribbed knit top with functional snap-button placket and raw edge hemline.', 399.00, 699.00, 2, 'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?auto=format&fit=crop&w=800&q=80', 65, 4.6, FALSE),
(10, 'High-Waist Wide Leg Pleated Trousers', 'Tailored wide-leg fluid trousers with twin pleats, high-rise fit and clean belt loops.', 899.00, 1399.00, 2, 'https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?auto=format&fit=crop&w=800&q=80', 25, 4.7, TRUE),
(11, 'Embroidered Cotton Kurti with Side Slits', 'Contemporary ethnic tunic featuring delicate thread embroidery on yoke and comfortable 3/4th sleeves.', 599.00, 999.00, 2, 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=800&q=80', 44, 4.4, FALSE),
(12, 'Oversized Pastel Drop-Shoulder Hoodie', 'Cosy fleece-lined kangaroo pocket pastel lilac hoodie designed for an ultra-relaxed aesthetic.', 899.00, 1399.00, 2, 'https://images.unsplash.com/photo-1556905055-8f358a7a47b2?auto=format&fit=crop&w=800&q=80', 20, 4.9, TRUE),
(13, 'Sleeveless Satin Cowl Neck Party Top', 'Silky lustrous satin cowl neck camisole with adjustable spaghetti straps for weekend glam.', 499.00, 799.00, 2, 'https://images.unsplash.com/photo-1564257631407-4deb1f99d992?auto=format&fit=crop&w=800&q=80', 30, 4.5, FALSE),
(14, 'Flared High-Rise Vintage Blue Jeans', 'Classic 70s inspired flare denim pants with sculpting high waist and clean back pocket design.', 1099.00, 1699.00, 2, 'https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=800&q=80', 15, 4.8, TRUE),

-- Kids Category
(15, 'Dino Explorer Graphic Cotton Tee (Boys)', 'Playful Jurassic print crewneck tee in 100% combed cotton, soft on sensitive skin.', 299.00, 499.00, 3, 'https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?auto=format&fit=crop&w=800&q=80', 55, 4.6, FALSE),
(16, 'Polka Dot Tiered Cotton Frock (Girls)', 'Cute twirl-worthy tiered dress with contrast Peter Pan collar and back button fastening.', 499.00, 799.00, 3, 'https://images.unsplash.com/photo-1622290291468-a28f7a7dc6a8?auto=format&fit=crop&w=800&q=80', 38, 4.7, TRUE),
(17, 'Active Fleece Joggers & Hoodie Set', '2-piece matching coordinates with elasticated track pants and zip-up hooded sweatshirt.', 799.00, 1199.00, 3, 'https://images.unsplash.com/photo-1503944583220-79d8926ad5e2?auto=format&fit=crop&w=800&q=80', 20, 4.8, TRUE),
(18, 'Elasticated Denim Pull-On Shorts', 'Comfortable stretch waistband denim shorts with faux fly and roll-up hem.', 349.00, 549.00, 3, 'https://images.unsplash.com/photo-1519457431-44ccd64a579b?auto=format&fit=crop&w=800&q=80', 40, 4.4, FALSE),
(19, 'Unicorn Magic Sparkle Sweatshirt', 'Vibrant pastel pullover with glitter unicorn motif and ribbed cuffs.', 449.00, 699.00, 3, 'https://images.unsplash.com/photo-1518831959646-742c3a14ebf7?auto=format&fit=crop&w=800&q=80', 35, 4.9, FALSE),

-- Footwear Category
(20, 'Retro Colorblock Chunky Street Sneakers', '90s-inspired lifestyle sneakers with padded ankle collar, cushioned EVA sole, and breathable mesh.', 1199.00, 1799.00, 4, 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=800&q=80', 24, 4.8, TRUE),
(21, 'Minimalist Clean White Tennis Low-Tops', 'Versatile all-white faux leather sneakers with durable cupsole and perforated side accents.', 999.00, 1499.00, 4, 'https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=800&q=80', 42, 4.7, TRUE),
(22, 'Cloud Foam Contoured Casual Slides', 'Water-resistant lightweight EVA molded slip-on sliders with ergonomic arch support.', 399.00, 599.00, 4, 'https://images.unsplash.com/photo-1603808033192-082d6919d3e1?auto=format&fit=crop&w=800&q=80', 60, 4.5, FALSE),
(23, 'Knit Slip-On Breathable Walkers', 'Ultra-light sock sneaker with flexible traction outsole, ideal for gym and daily commute.', 799.00, 1199.00, 4, 'https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?auto=format&fit=crop&w=800&q=80', 30, 4.4, FALSE),
(24, 'Canvas High-Top Skate Shoes', 'Durable black & white vulcanized rubber canvas shoes with reinforced toe cap.', 899.00, 1399.00, 4, 'https://images.unsplash.com/photo-1512374382149-233c42b6a83b?auto=format&fit=crop&w=800&q=80', 19, 4.6, FALSE),
(25, 'Strappy Summer Block Heel Sandals', 'Elegant 2-inch block heels with faux leather crossover straps and square open toe.', 899.00, 1299.00, 4, 'https://images.unsplash.com/photo-1543163521-1bf539c55dd2?auto=format&fit=crop&w=800&q=80', 16, 4.3, FALSE),

-- Accessories Category
(26, 'Matte Black Geometric Rectangular Sunglasses', 'UV400 protected tinted lenses in an acetate frame for a sleek cyber-aesthetic look.', 349.00, 599.00, 5, 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=800&q=80', 45, 4.7, TRUE),
(27, 'Structured Canvas Urban Sling Bag', 'Compact water-resistant chest cross-body sling with multiple zippered compartments and quick-release buckle.', 599.00, 899.00, 5, 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=800&q=80', 32, 4.6, TRUE),
(28, 'Washed Cotton Baseball Cap with NYC Embroidery', 'Classic curved visor dad cap with brass clasp adjuster and breathable eyelets.', 299.00, 449.00, 5, 'https://images.unsplash.com/photo-1588850561407-ed78c282e89b?auto=format&fit=crop&w=800&q=80', 50, 4.5, FALSE),
(29, 'Minimalist Stainless Steel Cuban Chain', 'Anti-tarnish silver finish chunky curb link neck chain with lobster claw clasp.', 299.00, 499.00, 5, 'https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?auto=format&fit=crop&w=800&q=80', 40, 4.6, FALSE),
(30, 'Pack of 3 Quarter Ankle Terry Cushion Socks', 'Sweat-wicking combed cotton ribbed sports socks with reinforced heel and toe.', 199.00, 299.00, 5, 'https://images.unsplash.com/photo-1586350977771-b3b0abd50c82?auto=format&fit=crop&w=800&q=80', 75, 4.4, FALSE),
(31, 'Faux Leather Minimalist Bi-Fold Wallet', 'Slim profile wallet with 6 card slots, currency divider, and coin pocket.', 399.00, 599.00, 5, 'https://images.unsplash.com/photo-1627123424574-724758594e93?auto=format&fit=crop&w=800&q=80', 35, 4.5, FALSE),
(32, 'Corduroy Bucket Hat in Olive Green', 'Trendy unstructured corduroy fisherman bucket hat for street-ready styling.', 349.00, 549.00, 5, 'https://images.unsplash.com/photo-1575428652377-a2d80e2277fc?auto=format&fit=crop&w=800&q=80', 25, 4.4, FALSE)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 5. Seed Sample Orders
INSERT INTO orders (id, user_id, total_amount, discount, status, payment_method, address, created_at) VALUES
(1001, 2, 1498.00, 149.80, 'Delivered', 'UPI', 'Flat 402, Sunshine Heights, MG Road, Bengaluru, Karnataka - 560001', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1002, 2, 899.00, 0.00, 'Shipped', 'CARD', 'Flat 402, Sunshine Heights, MG Road, Bengaluru, Karnataka - 560001', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1003, 3, 1299.00, 129.90, 'Packed', 'COD', 'House 12, Sector 15, Gurgaon, Haryana - 122001', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1004, 3, 499.00, 0.00, 'Placed', 'UPI', 'House 12, Sector 15, Gurgaon, Haryana - 122001', NOW())
ON DUPLICATE KEY UPDATE status=VALUES(status);

-- 6. Seed Sample Order Items
INSERT INTO order_items (order_id, product_id, size, quantity, price) VALUES
(1001, 1, 'L', 1, 499.00),
(1001, 2, '32', 1, 999.00),
(1002, 10, 'M', 1, 899.00),
(1003, 4, 'XL', 1, 1299.00),
(1004, 1, 'M', 1, 499.00)
ON DUPLICATE KEY UPDATE price=VALUES(price);

-- 7. Seed Sample Cart for Sakshi
INSERT INTO cart_items (user_id, product_id, size, quantity) VALUES
(2, 8, 'M', 1),
(2, 20, '8', 1)
ON DUPLICATE KEY UPDATE quantity=VALUES(quantity);

-- 8. Seed Sample Wishlist
INSERT INTO wishlist (user_id, product_id) VALUES
(2, 4),
(2, 12),
(3, 2)
ON DUPLICATE KEY UPDATE product_id=VALUES(product_id);
