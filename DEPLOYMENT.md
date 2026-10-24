# ZUDIO Production Deployment Guide

This guide provides step-by-step instructions to deploy the **ZUDIO Online Shopping Management System** to production environments, cloud providers (Render / Railway), and local Docker containers.

---

## 1. Running Locally with Docker Compose

Running the entire stack (Application + MySQL 8.4 Database) locally requires only Docker and Docker Compose.

### Prerequisites
- Docker Engine 24.0+ and Docker Compose v2.0+

### Steps
1. **Clone or navigate to the project directory:**
   ```bash
   cd zudio-shopping-system
   ```

2. **Inspect or configure environment settings:**
   The `docker-compose.yml` file is pre-configured with secure container networking. If needed, create a `.env` file from `.env.example`:
   ```bash
   cp .env.example .env
   ```

3. **Build and launch containers:**
   ```bash
   docker compose up --build -d
   ```

4. **Verify container status:**
   ```bash
   docker compose ps
   ```
   Both `zudio_mysql` and `zudio_app` should report `healthy` or `running`.

5. **Access the application:**
   - **Storefront & Admin:** Open [http://localhost:8080](http://localhost:8080)
   - **Health Endpoint:** [http://localhost:8080/health](http://localhost:8080/health)
   - **Initial Admin Login:** `admin@zudio.in` (configured via `ADMIN_PASSWORD` in `docker-compose.yml`)

6. **Shutting down:**
   ```bash
   docker compose down
   ```
   *(To wipe database volumes as well: `docker compose down -v`)*

---

## 2. Pushing to GitHub (Without Secrets)

1. **Verify `.gitignore` is active:**
   Ensure `.env`, `target/`, `tools/`, and logs are excluded:
   ```bash
   git status
   ```

2. **Initialize repository and commit:**
   ```bash
   git init
   git add .
   git commit -m "feat: production-ready Zudio application with PBKDF2, HMAC tokens, and Docker"
   ```

3. **Link to GitHub and push:**
   ```bash
   git branch -M main
   git remote add origin https://github.com/<your-username>/zudio-shopping-system.git
   git push -u origin main
   ```

---

## 3. Provisioning Managed MySQL (Railway or Aiven)

In production, running a managed cloud database provides automated backups, high availability, point-in-time recovery, and auto-scaling storage.

### Option A: Railway Managed MySQL
1. Go to [Railway.app](https://railway.app/) and sign in.
2. Click **New Project** → **Provision MySQL**.
3. Once provisioned, click on the MySQL service and navigate to the **Variables** or **Connect** tab.
4. Copy the connection parameters:
   - `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`
5. Formulate your JDBC URL:
   ```
   jdbc:mysql://<MYSQLHOST>:<MYSQLPORT>/<MYSQLDATABASE>?createDatabaseIfNotExist=true&useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
   ```

### Option B: Aiven for MySQL (Free Tier Available)
1. Go to [Aiven.io](https://aiven.io/) and create a free MySQL service.
2. Note the **Service URI** and **SSL CA certificate** (if required).
3. Append `sslMode=REQUIRED` to the JDBC URL for strict TLS transport security.

---

## 4. Deploying Web Service to Render or Railway

### Deploying on Railway
1. Inside your Railway project containing your MySQL instance, click **New Service** → **GitHub Repo**.
2. Select your `zudio-shopping-system` repository.
3. Railway automatically detects the `Dockerfile` and builds the multi-stage image.
4. In the **Variables** tab for the new Web Service, add the following environment variables:

| Variable Name | Recommended Value / Description |
| :--- | :--- |
| `PORT` | `8080` (or Railway default `$PORT`) |
| `APP_ENV` | `production` |
| `DB_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=true&serverTimezone=UTC` |
| `DB_USER` | `${{MySQL.MYSQLUSER}}` |
| `DB_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
| `TOKEN_SECRET` | A secure 64-character random string (e.g. generated via `openssl rand -hex 32`) |
| `ADMIN_EMAIL` | `admin@yourbrand.com` |
| `ADMIN_PASSWORD` | A strong password (min 12 chars, upper/lower/symbols) |
| `ALLOWED_ORIGIN` | `*` (or your custom domain) |

5. Under **Settings** → **Networking**, generate a public domain (e.g., `zudio-production.up.railway.app`).

### Deploying on Render
1. Go to [Render.com](https://render.com/) and click **New** → **Web Service**.
2. Connect your GitHub repository.
3. Choose **Docker** as the Environment / Runtime.
4. Set the Instance Type (Free / Starter).
5. In **Environment Variables**, provide `PORT`, `APP_ENV`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `TOKEN_SECRET`, `ADMIN_EMAIL`, and `ADMIN_PASSWORD`.
6. Set Health Check Path to: `/health`.
7. Click **Create Web Service**. Render builds the image and deploys the container.

---

## 5. Post-Deployment Verification Checklist

Once deployed to production, run through this verification checklist:

- [ ] **1. Probe Healthcheck Endpoint:**
  Visit `https://<your-app-domain>/health` in your browser.
  Ensure it responds with:
  ```json
  {"status":"UP","db":"UP"}
  ```
- [ ] **2. Verify TLS / HTTPS:**
  Ensure the URL uses `https://` with a valid SSL/TLS certificate (automatically provided by Render/Railway).
- [ ] **3. Initial Admin Login:**
  Navigate to `https://<your-app-domain>/#/admin` and log in with your configured `ADMIN_EMAIL` and `ADMIN_PASSWORD`.
- [ ] **4. Customer Registration & Shopping Flow:**
  - Open an Incognito window, register a new customer account.
  - Browse catalog, filter by category and price.
  - Add items with selected sizes to cart.
  - Apply coupon `ZUDIO10` (10% off orders over ₹500) or `ZUDIO20` (20% off orders over ₹1000).
  - Proceed to checkout, enter shipping address, select COD/UPI/Card, and complete payment.
- [ ] **5. Order Status Tracking:**
  - View order timeline in `/ #/orders`.
  - In Admin tab, open Orders Management, update order status to `Packed` → `Shipped` → `Delivered`.
  - Refresh customer tab and verify real-time status progression.
- [ ] **6. Run Security Automated Test Suite:**
  Run `test_security.ps1` against the live deployed URL:
  ```powershell
  # Edit $baseUrl in test_security.ps1 to point to your live domain
  powershell -ExecutionPolicy Bypass -File test_security.ps1
  ```

---

## 6. Known Limitations & Production Architecture Recommendations

1. **Managed Database vs. Container Database:**
   - In production, never run single-instance MySQL containers with ephemeral volumes. Use a managed service (AWS RDS, Google Cloud SQL, Railway MySQL, or Aiven) for automatic daily snapshots, replicas, and automated point-in-time recovery.
2. **Reverse Proxy & HTTPS Termination:**
   - The application server uses Java's built-in `HttpServer`. In enterprise production, place the application behind a reverse proxy (Nginx, Cloudflare, AWS ALB, or Render/Railway Edge) which handles:
     - TLS 1.3 termination
     - DDoS mitigation & HTTP request buffering
     - Global edge caching of static assets (`.css`, `.js`, `.svg`)
3. **Horizontal Scaling & Session State:**
   - Because user sessions use stateless, cryptographically signed HMAC-SHA256 tokens (`v1.<payload>.<sig>`), the backend application is **100% stateless** and can be horizontally scaled across multiple container replicas without requiring sticky sessions or distributed session caches.
4. **Database Migration Strategy:**
   - The application includes built-in idempotent schema generation (`DatabaseUtil.java`), which safely creates tables and initial seed data if they do not exist. For large-scale multi-developer production migrations, consider adopting Flyway or Liquibase for versioned delta migrations.
