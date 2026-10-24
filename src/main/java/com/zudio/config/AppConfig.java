package com.zudio.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Enterprise environment and application configuration loader.
 * Prioritizes OS Environment Variables first, System Properties second,
 * and falls back to config.properties only for local development.
 */
public class AppConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            System.err.println("[WARN] Failed to load config.properties from classpath: " + e.getMessage());
        }
    }

    public static String get(String key, String defaultValue) {
        // 1. Exact Environment Variable (e.g. DB_URL, TOKEN_SECRET, PORT)
        String envKey = key.toUpperCase().replace('.', '_');
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) return envVal.trim();

        // Also try direct key as env var if already in SNAKE_CASE
        String directEnv = System.getenv(key);
        if (directEnv != null && !directEnv.isBlank()) return directEnv.trim();

        // 2. System Property (e.g. -Dserver.port=8080)
        String sysVal = System.getProperty(key);
        if (sysVal != null && !sysVal.isBlank()) return sysVal.trim();

        // 3. Properties File
        String propVal = props.getProperty(key);
        if (propVal != null && !propVal.isBlank()) return propVal.trim();

        return defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String val = get(key, null);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean isProduction() {
        String env = get("APP_ENV", get("ENV", "development"));
        return "production".equalsIgnoreCase(env) || "prod".equalsIgnoreCase(env);
    }

    public static int getServerPort() {
        // Supports PORT or server.port
        return getInt("PORT", getInt("server.port", 8080));
    }

    public static String getServerHost() {
        return get("HOST", get("server.host", "0.0.0.0"));
    }

    public static String getDbDriver() {
        return get("DB_DRIVER", get("db.driver", "com.mysql.cj.jdbc.Driver"));
    }

    public static String getDbUrl() {
        return get("DB_URL", get("db.url", "jdbc:mysql://localhost:3306/zudio_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"));
    }

    public static String getDbUser() {
        return get("DB_USER", get("db.user", "root"));
    }

    public static String getDbPassword() {
        return get("DB_PASSWORD", get("db.password", ""));
    }

    public static String getTokenSecret() {
        String secret = get("TOKEN_SECRET", get("token.secret", null));
        if (secret == null || secret.isBlank()) {
            if (isProduction()) {
                throw new IllegalStateException("CRITICAL SECURITY ERROR: 'TOKEN_SECRET' environment variable must be set in production!");
            }
            // In dev mode, return a stable local dev fallback secret
            return "zudio_dev_local_secret_key_change_in_production_min32char";
        }
        return secret;
    }

    public static String getAdminEmail() {
        return get("ADMIN_EMAIL", get("admin.email", "admin@zudio.in"));
    }

    public static String getAdminPassword() {
        return get("ADMIN_PASSWORD", get("admin.password", "Admin@Zudio2026!"));
    }

    public static String getAllowedOrigin() {
        return get("ALLOWED_ORIGIN", get("cors.allowedOrigin", "*"));
    }

    public static String getFrontendDir() {
        return get("FRONTEND_DIR", get("frontend.dir", "frontend"));
    }
}
