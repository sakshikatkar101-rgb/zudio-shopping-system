# ZUDIO – Online Shopping Management System 🛍️
> A complete, lightweight, full-stack web application inspired by the trendy Indian fast-fashion retail brand **Zudio**. Engineered with **Core Java 17+ (built-in `HttpServer`)**, **pure JDBC**, **MySQL 8.4**, and **vanilla HTML5/CSS3/JavaScript (ES6+)**.

---

## 🌐 Live Production Deployment
- **Live Application URL:** [https://zudio-shopping.onrender.com](https://zudio-shopping.onrender.com)
- **Live Healthcheck Endpoint:** [https://zudio-shopping.onrender.com/health](https://zudio-shopping.onrender.com/health) (Returns `{"status":"UP","db":"UP"}`)
- **Cloud Host:** Render Web Service (Free Tier, Docker Runtime)
- **Managed Database:** Aiven Cloud MySQL 8.4.11 with strict TLS encryption (`sslMode=REQUIRED`)
- **Source Repository:** [sakshikatkar101-rgb/zudio-shopping-system](https://github.com/sakshikatkar101-rgb/zudio-shopping-system)

---

## 🌟 Key Highlights & Engineering Features
- **Zero Heavy Frameworks:** No Spring, Spring Boot, Hibernate, or external ORM.
- **Pure JDBC with Connection Pool:** Handcrafted connection pool (10 connections), 10-attempt retry loop (3s delay) for resilient cloud DB connectivity, and pure parameterized SQL via `PreparedStatement` (SQL injection immunity).
- **Embedded Web Server:** Utilizes Java 17's built-in `com.sun.net.httpserver.HttpServer` multi-threaded executor serving both JSON REST endpoints and static assets on port `8080`.
- **Enterprise Security Hardening:**
  - Password hashing with **PBKDF2WithHmacSHA256** (120,000 iterations, unique 16-byte random salt per user).
  - Cryptographically signed **HMAC-SHA256 tokens** (`v1.<payload>.<sig>`) with 24-hour expiration.
  - Server-side price integrity enforcement (discards client-forged prices).
  - Anti-path traversal canonical resolution & file extension blocklist.
  - Universal XSS HTML escaping across all DOM renderers.
  - Sliding-window login rate limiting (5 failed attempts per 10 mins).
  - Strict security headers (`CSP`, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`).
  - Automated security verification suite (`test_security.ps1`) passing 22/22 tests (100%).
- **Packaging & Portability:** Single executable fat JAR (`target/zudio.jar`) built with `maven-shade-plugin`, multi-stage Dockerfile, and `docker-compose.yml` with MySQL 8.4 service.
- **Production Health Monitoring:** `GET /health` endpoint reporting `{"status":"UP","db":"UP"}`.
- **ACID Transaction Control:** Order placement decrements inventory stock and creates order records within an atomic database transaction with `commit` and `rollback`.
- **Order Cancellation & Stock Restoration:** Customers (and administrators) can cancel orders in `Placed` or `Packed` status. Stock is atomically restored to products, status changes to `Cancelled`, reason and timestamp are saved, and simulated refunds are initiated for online payments.
- **Realistic Simulated Payments:** Interactive Card modal (auto-formatting, brand detection for Visa/Mastercard/RuPay/Amex, live preview card) and UPI (quick handles `@oksbi`, `@okhdfcbank`, `@paytm`, `@ybl`) with simulated gateway authorization delays. Zero sensitive card data is ever stored or transmitted.
- **Pure SVG Charting:** Custom sales velocity trend graph crafted with raw SVG bezier curves (zero external chart libraries). Non-cancelled orders are accurately filtered.
- **Single Page Application (SPA):** Hash routing (`#/home`, `#/shop`, `#/product/:id`, `#/cart`, `#/checkout`, `#/orders`, `#/admin`) with smooth micro-interactions.

---

## 🏗️ Technology Stack

| Layer | Technology | Purpose |
| :--- | :--- | :--- |
| **Backend** | Core Java 17+ | Application business logic & RESTful routing |
| **Server** | `com.sun.net.httpserver.HttpServer` | Built-in lightweight multi-threaded HTTP server |
| **Database Access** | Pure JDBC (`java.sql.*`) | Connection pool & `PreparedStatement` queries |
| **Database** | MySQL 8.x / MariaDB | Relational persistence & foreign key integrity |
| **Frontend** | Vanilla HTML5, CSS3, JavaScript (ES6+) | Editorial UI, glassmorphism design, SPA routing |
| **Build & Tooling** | Apache Maven | Minimal dependencies: `mysql-connector-j` & `Gson` |
| **Containerization** | Docker & Docker Compose | Multi-stage image build & orchestrated deployment |

> **Database Compatibility Note:** Schema and seed scripts are standard MySQL 8.x compatible. Local development and automated testing were verified against MariaDB (100% protocol and syntax compatible with MySQL 8).

---

## 🚀 Quick Start Guide

### Prerequisites
1. **Java Development Kit (JDK 17 or higher)**
2. **Apache Maven (3.8+)**
3. **MySQL 8.x Server** running on `localhost:3306` (or Docker)

### Option 1: Docker Compose (Easiest)
```bash
# Builds fat JAR in container and brings up MySQL 8.4 + App
docker compose up --build -d

# Open in browser: http://localhost:8080
```

### Option 2: Pre-Built Fat JAR
```bash
# Package fat JAR
mvn clean package -DskipTests

# Run anywhere (reads static assets from inside the JAR)
java -jar target/zudio.jar
```

### Option 3: Local Maven Run
```bash
# Compile and run
mvn compile exec:java

# Or on Windows:
run.bat
# Or in PowerShell:
.\run.ps1
# Or on Linux / macOS:
./run.sh
```

---

## 🔑 Initial Credentials

| Role | Email | Password | Access Privileges |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin@zudio.in` | Configured via `ADMIN_PASSWORD` (Default: `Admin@Zudio2026!`) | Executive KPI dashboard, Sales chart, Product CRUD, Order fulfillment status updater, Customer list |
| **Sample Customer 1** | `sakshi@example.com` | `pass123` | Shopping bag, Wishlist, Checkout, Order tracking timeline |
| **Sample Customer 2** | `rohit@example.com` | `pass123` | Shopping bag, Wishlist, Checkout, Order tracking timeline |

> **Security Note:** User passwords are encrypted on-the-fly at runtime using `PBKDF2WithHmacSHA256` (120k iterations). No plaintext or hardcoded password hashes exist in `seed.sql`.

---

## 🛡️ Security & Deployment Documentation
- **[SECURITY_AUDIT.md](SECURITY_AUDIT.md)**: Full security audit report, vulnerability breakdown, remediation details, and automated test suite results.
- **[DEPLOYMENT.md](DEPLOYMENT.md)**: Complete deployment guide for Docker Compose, GitHub, Render, Railway, and managed cloud MySQL (Aiven).

---

## 🧪 Automated Security Test Suite

To run the automated security test suite against a running server:
```powershell
powershell -ExecutionPolicy Bypass -File test_security.ps1
```
This executes 32 automated vulnerability and integrity probes covering:
1. Directory path traversal & protected file blocking (HTTP 403)
2. Privilege escalation & customer-to-admin RBAC (HTTP 403)
3. Insecure Direct Object Reference (IDOR) on orders (HTTP 403)
4. Client-side price tampering detection (Server DB price enforcement)
5. SQL Injection attack vectors (clean parameterized handling)
6. Stored XSS defense & HTML entity sanitization
7. Brute-force credential lockout (HTTP 429 after 5 failed attempts)
8. Production `/health` probe (HTTP 200 UP)
9. Anti-IDOR order cancellation defense (HTTP 403)
10. Atomic inventory stock decrement on checkout & restoration on cancellation
11. Simulated card and payment status persistence (`PAID`, `REFUND_PENDING`)
12. Duplicate cancellation prevention (HTTP 400)
13. Dispatched order (Shipped/Delivered) cancellation rejection (HTTP 400)

---

## 📡 REST API Documentation

### System Health
- `GET /health` – Returns `{"status":"UP","db":"UP"}`

### Authentication (`/api/auth`)
- `POST /api/auth/register` – Create customer account (`{ name, email, password, phone }`)
- `POST /api/auth/login` – Authenticate & receive Bearer session token (`{ email, password }`)
- `GET  /api/auth/me` – Retrieve profile of authenticated user
- `POST /api/auth/logout` – Invalidate active session token

### Products & Categories (`/api/products`, `/api/categories`)
- `GET /api/categories` – Fetch all fashion departments
- `GET /api/products` – Catalog search & filtering (`?category=Men&search=tee&minPrice=200&maxPrice=1000&sortBy=price_asc&trending=true`)
- `GET /api/products/{id}` – Get detailed product specs and stock

### Shopping Bag (`/api/cart`)
- `GET    /api/cart` – View active cart items with subtotal, discount & shipping
- `POST   /api/cart` – Add product (`{ productId, size, quantity }`)
- `PUT    /api/cart/{id}` – Update item quantity (`{ quantity }`)
- `DELETE /api/cart/{id}` – Remove item from bag
- `DELETE /api/cart/clear` – Empty entire bag

### Orders (`/api/orders`)
- `GET  /api/orders` – View order history with fulfillment timeline and payment badges
- `GET  /api/orders/{id}` – View single order breakdown
- `POST /api/orders` – Place order with ACID transaction & stock reduction (`{ paymentMethod, address, couponCode, paymentStatus, cardLast4, cardBrand }`)
- `POST /api/orders/{id}/cancel` – Cancel order, restore stock, initiate simulated refund (`{ reason }`)

### Saved Wishlist (`/api/wishlist`)
- `GET    /api/wishlist` – View customer's saved items
- `POST   /api/wishlist` – Toggle product in wishlist (`{ productId }`)
- `DELETE /api/wishlist/{productId}` – Remove product from wishlist

### Coupons (`/api/coupons`)
- `POST /api/coupons/validate` – Check coupon validity and discount amount (`{ code, orderAmount }`)
- `GET  /api/coupons` – List active promo coupons

### Admin Console (`/api/admin/*` – Admin Token Required)
- `GET    /api/admin/dashboard` – KPI metrics (Revenue, Orders, Low Stock) + Sales Trend array
- `GET    /api/admin/products` – Complete catalog inventory list
- `POST   /api/admin/products` – Add new fashion drop (`{ name, price, mrp, categoryId, stock, imageUrl, description, isTrending }`)
- `PUT    /api/admin/products/{id}` – Edit product details & stock
- `DELETE /api/admin/products/{id}` – Delete product
- `GET    /api/admin/orders` – All orders placed across store
- `PUT    /api/admin/orders/{id}` – Update fulfillment status (`{ status: "Placed"|"Packed"|"Shipped"|"Delivered"|"Cancelled" }`)
- `GET    /api/admin/customers` – Customer database directory
