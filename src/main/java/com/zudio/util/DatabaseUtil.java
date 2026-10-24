package com.zudio.util;

import com.zudio.config.AppConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Enterprise Pure-JDBC Connection Manager & Connection Pool.
 * Features:
 * - 10-attempt resilient retry loop (3 seconds apart) for managed cloud databases
 * - Fixed connection pool (Max: 10 connections)
 * - Idempotent schema initialization
 * - Dynamic PBKDF2 password generation for admin and sample users (no hardcoded hashes)
 * - Full MySQL 8 and MariaDB protocol compatibility
 */
public class DatabaseUtil {
    private static final int MAX_POOL_SIZE = 10;
    private static final int RETRY_ATTEMPTS = 10;
    private static final int RETRY_DELAY_MS = 3000;

    private static final BlockingQueue<Connection> connectionPool = new ArrayBlockingQueue<>(MAX_POOL_SIZE);
    private static boolean initialized = false;

    static {
        try {
            Class.forName(AppConfig.getDbDriver());
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] JDBC Driver not found: " + AppConfig.getDbDriver());
        }
    }

    /**
     * Initializes the connection pool and checks/creates database tables.
     */
    public static synchronized void initialize() {
        if (initialized) return;

        System.out.println("[DB] Connecting to database at: " + AppConfig.getDbUrl());

        Connection initialConn = null;
        for (int attempt = 1; attempt <= RETRY_ATTEMPTS; attempt++) {
            try {
                initialConn = createRawConnection();
                if (initialConn != null && initialConn.isValid(3)) {
                    System.out.println("[DB] Successfully connected to database on attempt " + attempt + ".");
                    break;
                }
            } catch (SQLException e) {
                System.err.println("[DB Connection Attempt " + attempt + "/" + RETRY_ATTEMPTS + " Failed]: " + e.getMessage());
                if (attempt < RETRY_ATTEMPTS) {
                    System.out.println("[DB] Waiting " + (RETRY_DELAY_MS / 1000) + " seconds before retrying...");
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Database connection interrupted", ie);
                    }
                } else {
                    throw new RuntimeException("Failed to connect to MySQL database after " + RETRY_ATTEMPTS + " attempts.", e);
                }
            }
        }

        try {
            // Idempotent schema and seed check
            ensureDatabaseAndSchema(initialConn);
            initialConn.close();

            // Populate connection pool up to 3 warm connections
            for (int i = 0; i < 3; i++) {
                connectionPool.offer(createRawConnection());
            }
            initialized = true;
            System.out.println("[DB] Connection pool initialized (Active pool: " + connectionPool.size() + "/" + MAX_POOL_SIZE + ").");

        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failure", e);
        }
    }

    private static Connection createRawConnection() throws SQLException {
        String url = AppConfig.getDbUrl();
        String user = AppConfig.getDbUser();
        String pass = AppConfig.getDbPassword();
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * Borrows a connection from the pool or creates a fresh one.
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            initialize();
        }
        try {
            Connection conn = connectionPool.poll(3, TimeUnit.SECONDS);
            if (conn != null && !conn.isClosed() && conn.isValid(2)) {
                return conn;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return createRawConnection();
    }

    /**
     * Returns a borrowed connection back to the pool.
     */
    public static void releaseConnection(Connection conn) {
        if (conn == null) return;
        try {
            if (!conn.isClosed() && conn.isValid(1) && conn.getAutoCommit()) {
                if (!connectionPool.offer(conn)) {
                    conn.close(); // Pool full, close it
                }
            } else {
                conn.close();
            }
        } catch (SQLException e) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    /**
     * Probes database connectivity for health checks.
     */
    public static boolean isHealthy() {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn != null && !conn.isClosed()) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1")) {
                    return rs.next();
                }
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (conn != null) {
                releaseConnection(conn);
            }
        }
        return false;
    }

    /**
     * Checks if tables exist in the database, and executes schema.sql + seed.sql if empty.
     */
    private static void ensureDatabaseAndSchema(Connection conn) {
        try {
            boolean tablesExist = false;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'products'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        tablesExist = true;
                    }
                }
            }

            if (!tablesExist) {
                System.out.println("[DB] Initializing database schema from schema.sql...");
                executeSqlScript(conn, "sql/schema.sql");
            }

            // Check if catalog products are seeded
            boolean seeded = false;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM products")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        seeded = true;
                    }
                }
            } catch (SQLException ignored) {}

            if (!seeded) {
                System.out.println("[DB] Seeding catalog products, categories, and coupons from seed.sql...");
                executeSqlScript(conn, "sql/seed.sql");
            }

            // Dynamically seed or ensure Administrator & Sample Customer accounts with PBKDF2 hashes
            ensureDefaultUsers(conn);

        } catch (SQLException e) {
            System.err.println("[DB ERROR during schema setup]: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Dynamically creates admin and sample customer accounts if missing.
     * Passwords are encrypted on-the-fly using PBKDF2WithHmacSHA256 (120,000 iterations).
     */
    private static void ensureDefaultUsers(Connection conn) {
        String adminEmail = AppConfig.getAdminEmail().trim().toLowerCase();
        String adminPass = AppConfig.getAdminPassword();

        try {
            // 1. Ensure Admin Account
            boolean adminExists = false;
            try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE email = ?")) {
                ps.setString(1, adminEmail);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) adminExists = true;
                }
            }

            if (!adminExists) {
                System.out.println("[DB] Dynamically creating Administrator account (" + adminEmail + ") with PBKDF2 hash...");
                String adminHash = PasswordUtil.hashPassword(adminPass);
                String sql = "INSERT INTO users (name, email, password_hash, role, phone) VALUES (?, ?, ?, 'ADMIN', ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, "Zudio Administrator");
                    ps.setString(2, adminEmail);
                    ps.setString(3, adminHash);
                    ps.setString(4, "+91 98765 43210");
                    ps.executeUpdate();
                }
            }

            // 2. Ensure Sample Customers
            ensureCustomer(conn, "Sakshi Sharma", "sakshi@example.com", "pass123", "+91 98111 22334");
            ensureCustomer(conn, "Rohit Verma", "rohit@example.com", "pass123", "+91 97222 33445");

        } catch (SQLException e) {
            System.err.println("[DB User Seeding Notice]: " + e.getMessage());
        }
    }

    private static void ensureCustomer(Connection conn, String name, String email, String password, String phone) throws SQLException {
        boolean exists = false;
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE email = ?")) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) exists = true;
            }
        }
        if (!exists) {
            String hash = PasswordUtil.hashPassword(password);
            String sql = "INSERT INTO users (name, email, password_hash, role, phone) VALUES (?, ?, ?, 'CUSTOMER', ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, email.trim().toLowerCase());
                ps.setString(3, hash);
                ps.setString(4, phone);
                ps.executeUpdate();
            }
        }
    }

    /**
     * Executes SQL statements from a file or classpath resource.
     */
    public static void executeSqlScript(Connection conn, String scriptPath) {
        String content = readScriptContent(scriptPath);
        if (content == null || content.isBlank()) {
            System.err.println("[DB] Could not find SQL script: " + scriptPath);
            return;
        }

        String[] statements = content.split(";\\s*(\\r?\\n)+");
        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                String trimmed = stripComments(sql.trim());
                if (!trimmed.isEmpty()) {
                    try {
                        stmt.execute(trimmed);
                    } catch (SQLException e) {
                        if (!e.getMessage().contains("already exists") && !e.getMessage().contains("Duplicate")) {
                            System.err.println("[DB SQL Notice]: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB Script Execution Error]: " + e.getMessage());
        }
    }

    private static String stripComments(String sql) {
        StringBuilder sb = new StringBuilder();
        String[] lines = sql.split("\\r?\\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.startsWith("--") && !trimmedLine.startsWith("/*") && !trimmedLine.startsWith("#")) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private static String readScriptContent(String scriptPath) {
        Path path = Paths.get(scriptPath);
        if (Files.exists(path)) {
            try {
                return Files.readString(path, StandardCharsets.UTF_8);
            } catch (Exception ignored) {}
        }

        try (InputStream in = DatabaseUtil.class.getClassLoader().getResourceAsStream(scriptPath)) {
            if (in != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    return sb.toString();
                }
            }
        } catch (Exception ignored) {}

        return null;
    }
}
