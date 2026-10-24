package com.zudio.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

/**
 * Military-grade Password Hashing utility using PBKDF2WithHmacSHA256.
 * Format: <iterations>:<hexSalt>:<hexHash>
 * Uses 120,000 iterations and a 16-byte cryptographically secure random salt per user.
 */
public class PasswordUtil {
    public static final int ITERATIONS = 120_000;
    public static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Hashes a raw password using PBKDF2WithHmacSHA256 with 120,000 iterations.
     * Returns: iterations:hexSalt:hexHash
     */
    public static String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        String saltHex = bytesToHex(salt);
        String hashHex = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
        return ITERATIONS + ":" + saltHex + ":" + hashHex;
    }

    /**
     * Verifies a raw password against the stored iterations:salt:hash string.
     * Performs constant-time comparison to prevent timing attacks.
     */
    public static boolean verifyPassword(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        // Standard PBKDF2 format: <iterations>:<hexSalt>:<hexHash>
        if (storedHash.contains(":")) {
            String[] parts = storedHash.split(":", 3);
            if (parts.length == 3) {
                try {
                    int iterations = Integer.parseInt(parts[0]);
                    String saltHex = parts[1];
                    String expectedHashHex = parts[2];
                    byte[] salt = hexToBytes(saltHex);
                    String computedHashHex = pbkdf2(rawPassword.toCharArray(), salt, iterations, KEY_LENGTH_BITS);
                    return MessageDigest.isEqual(
                            computedHashHex.getBytes(StandardCharsets.UTF_8),
                            expectedHashHex.getBytes(StandardCharsets.UTF_8)
                    );
                } catch (Exception e) {
                    System.err.println("[PasswordUtil] PBKDF2 verification error: " + e.getMessage());
                    return false;
                }
            }
        }

        // Legacy format fallback during migration: <salt>$<sha256Hash>
        if (storedHash.contains("$")) {
            String[] parts = storedHash.split("\\$", 2);
            String saltHex = parts[0];
            String expectedHashHex = parts[1];
            String actualHashHex = computeLegacySha256(saltHex + rawPassword);
            return MessageDigest.isEqual(
                    actualHashHex.getBytes(StandardCharsets.UTF_8),
                    expectedHashHex.getBytes(StandardCharsets.UTF_8)
            );
        }

        return false;
    }

    private static String pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("PBKDF2 hashing failed", e);
        }
    }

    private static String computeLegacySha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
