package com.zudio.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zudio.config.AppConfig;
import com.zudio.model.User;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cryptographically signed Token Manager using HMAC-SHA256.
 * Generates and verifies tamper-proof session tokens containing claims, role, and 24h expiration.
 */
public class TokenUtil {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TOKEN_VALIDITY_MS = 24L * 60 * 60 * 1000; // 24 Hours
    private static final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

    public static class SessionUser {
        private final int userId;
        private final String email;
        private final String name;
        private final String role;
        private final long expiresAt;

        public SessionUser(int userId, String email, String name, String role, long expiresAt) {
            this.userId = userId;
            this.email = email;
            this.name = name;
            this.role = role;
            this.expiresAt = expiresAt;
        }

        public int getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getName() { return name; }
        public String getRole() { return role; }
        public long getExpiresAt() { return expiresAt; }
        public boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
        public boolean isAdmin() { return "ADMIN".equalsIgnoreCase(role); }
    }

    /**
     * Generates an HMAC-SHA256 signed token for an authenticated user.
     * Format: v1.<base64UrlPayload>.<base64UrlSignature>
     */
    public static String createToken(User user) {
        long now = System.currentTimeMillis();
        long exp = now + TOKEN_VALIDITY_MS;

        JsonObject payload = new JsonObject();
        payload.addProperty("uid", user.getId());
        payload.addProperty("email", user.getEmail());
        payload.addProperty("name", user.getName());
        payload.addProperty("role", user.getRole() != null ? user.getRole() : "CUSTOMER");
        payload.addProperty("iat", now);
        payload.addProperty("exp", exp);

        String payloadJson = payload.toString();
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String messageToSign = "v1." + encodedPayload;

        String signature = sign(messageToSign, AppConfig.getTokenSecret());
        return messageToSign + "." + signature;
    }

    /**
     * Validates signature and expiration of an incoming token.
     * Returns SessionUser if valid; otherwise null.
     */
    public static SessionUser validateToken(String token) {
        if (token == null || token.isBlank()) return null;

        String trimmed = token.trim();

        // Check if token was explicitly revoked/logged out
        if (revokedTokens.containsKey(trimmed)) {
            return null;
        }

        String[] parts = trimmed.split("\\.");
        if (parts.length != 3 || !"v1".equals(parts[0])) {
            return null;
        }

        String encodedPayload = parts[1];
        String providedSignature = parts[2];
        String messageToVerify = "v1." + encodedPayload;

        // Verify cryptographic HMAC-SHA256 signature
        String expectedSignature = sign(messageToVerify, AppConfig.getTokenSecret());
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8))) {
            System.err.println("[Security Alert] Token signature mismatch / tampering detected!");
            return null;
        }

        // Decode and parse payload
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedPayload);
            String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(payloadJson).getAsJsonObject();

            long exp = json.get("exp").getAsLong();
            if (System.currentTimeMillis() > exp) {
                // Token has expired
                return null;
            }

            int uid = json.get("uid").getAsInt();
            String email = json.get("email").getAsString();
            String name = json.get("name").getAsString();
            String role = json.get("role").getAsString();

            return new SessionUser(uid, email, name, role, exp);

        } catch (Exception e) {
            System.err.println("[Security Warning] Corrupted token payload: " + e.getMessage());
            return null;
        }
    }

    public static void removeToken(String token) {
        if (token != null && !token.isBlank()) {
            revokedTokens.put(token.trim(), System.currentTimeMillis() + TOKEN_VALIDITY_MS);
            // Periodic cleanup of expired revoked entries
            if (revokedTokens.size() > 500) {
                long now = System.currentTimeMillis();
                revokedTokens.entrySet().removeIf(e -> e.getValue() < now);
            }
        }
    }

    public static String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) return null;
        String trimmed = authHeader.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmed.substring(7).trim();
        }
        return trimmed;
    }

    private static String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 signature calculation failed", e);
        }
    }
}
