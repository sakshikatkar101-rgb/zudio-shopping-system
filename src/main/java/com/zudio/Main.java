package com.zudio;

import com.sun.net.httpserver.HttpServer;
import com.zudio.config.AppConfig;
import com.zudio.handler.*;
import com.zudio.util.DatabaseUtil;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        int port = AppConfig.getServerPort();

        System.out.println("===============================================================");
        System.out.println("  ZZZZZZ  U     U  DDDDD   I   OOOO   ");
        System.out.println("      Z   U     U  D    D  I  O    O  ");
        System.out.println("     Z    U     U  D    D  I  O    O  ");
        System.out.println("    Z     U     U  D    D  I  O    O  ");
        System.out.println("   Z       U   U   D    D  I  O    O  ");
        System.out.println("  ZZZZZZ    UUU    DDDDD   I   OOOO   ");
        System.out.println("  Online Shopping Management System (Lightweight Full-Stack)");
        System.out.println("  Stack: Java 17+ (HttpServer), Pure JDBC, MySQL 8, Vanilla JS");
        System.out.println("===============================================================");

        try {
            // 1. Initialize Database & Schema/Seed
            DatabaseUtil.initialize();

            // 2. Create Built-in HTTP Server (Binds to 0.0.0.0 or configured HOST)
            String host = AppConfig.getServerHost();
            HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);

            // 3. Register Health Endpoint
            server.createContext("/health", new HealthHandler());

            // 4. Register REST API Endpoints
            server.createContext("/api/auth", new AuthHandler());
            server.createContext("/api/products", new ProductHandler());
            server.createContext("/api/categories", new ProductHandler());
            server.createContext("/api/cart", new CartHandler());
            server.createContext("/api/wishlist", new WishlistHandler());
            server.createContext("/api/coupons", new CouponHandler());
            server.createContext("/api/orders", new OrderHandler());
            server.createContext("/api/admin", new AdminHandler());

            // 5. Register Static Frontend File Handler (serves index.html, css, js, assets)
            server.createContext("/", new StaticFileHandler());

            // 6. Multi-threaded Executor
            server.setExecutor(Executors.newFixedThreadPool(20));

            // 7. Start Server
            server.start();

            System.out.println();
            System.out.println(">>> SERVER STARTED SUCCESSFULLY! <<<");
            System.out.println(">>> App URL:   http://" + ("0.0.0.0".equals(host) ? "localhost" : host) + ":" + port);
            System.out.println(">>> API Base:  http://" + ("0.0.0.0".equals(host) ? "localhost" : host) + ":" + port + "/api/");
            System.out.println(">>> Health:    http://" + ("0.0.0.0".equals(host) ? "localhost" : host) + ":" + port + "/health");
            System.out.println(">>> Admin:     " + AppConfig.getAdminEmail() + " (Password set via ADMIN_PASSWORD / config)");
            System.out.println(">>> Customer:  sakshi@example.com / pass123 (Sample)");
            System.out.println(">>> Press Ctrl+C in console to stop.");
            System.out.println("===============================================================");

        } catch (Exception e) {
            System.err.println("Fatal server startup failure: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
